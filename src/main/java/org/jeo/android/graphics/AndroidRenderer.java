/* Copyright 2013 The jeo project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jeo.android.graphics;

import android.graphics.*;
import android.graphics.Rect;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import org.jeo.data.TileDataset;
import org.jeo.feature.Feature;
import org.jeo.geom.CoordinatePath;
import org.jeo.map.*;
import org.jeo.map.render.BaseRenderer;
import org.jeo.map.render.Label;
import org.jeo.tile.Tile;
import org.jeo.tile.TileCover;
import org.jeo.tile.TilePyramid;
import org.jeo.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static org.jeo.android.graphics.Graphics.*;
import static org.jeo.map.CartoCSS.*;

/**
 * Renders a map to an Android {@link Canvas}.
 * <p>
 * Usage:
 * <pre><code>
 * Canvas canvas = ...;
 * Map map = ...;
 *
 * AndroidRenderer r = new AndroidRenderer(canvas);
 * r.init(map);
 * r.render();
 * 
 * </code></pre>
 * </p>
 * @author Justin Deoliveira, OpenGeo
 */
public class AndroidRenderer extends BaseRenderer {

    static Logger LOG = LoggerFactory.getLogger(AndroidRenderer.class);

    /** the transformation pipeline */
    TransformPipeline tx;

    /** canvas to draw to */
    Canvas canvas;

    /** underlying bitmap */
    Bitmap bitmap;

    public AndroidRenderer(Canvas canvas) {
        this(canvas, null);
    }

    public AndroidRenderer(Canvas canvas, Bitmap bitmap) {
        this.canvas = canvas;
        this.bitmap = bitmap;
    }

    public TransformPipeline getTransform() {
        return tx;
    }

    @Override
    protected boolean canRenderVectors() {
        return true;
    }

    @Override
    protected boolean canRenderRasters() {
        return true;
    }

    @Override
    protected boolean canRenderTiles() {
        return true;
    }


    @Override
    public void init(View view, java.util.Map<?, Object> opts) {
        // initialize the transformation from world to screen
        tx = new TransformPipeline(view);
        tx.apply(canvas);

        super.init(view, opts);
    }

    @Override
    protected org.jeo.map.render.Labeller createLabeller() {
        return new Labeller(canvas, tx);
    }

    protected void render(TileDataset data, RuleList rules) {
        tx.reset(canvas);

        Rule rule = rules.collapse();

        try {
            TilePyramid pyr = data.pyramid();

            TileCover cov = pyr.cover(view.getBounds(), view.getWidth(), view.getHeight());
            cov.fill(data);

            Rect dst = new Rect();

            Paint p = paint(null, rule);

            double scx = cov.getGrid().getXRes() / view.iscaleX();
            double scy = cov.getGrid().getYRes() / view.iscaleY();

            dst.left = 0;
            for (int x = 0; x < cov.getWidth(); x++) {
                dst.bottom = canvas.getHeight();

                for (int y = 0; y < cov.getHeight(); y++) {
                    Tile t = cov.tile(x, y);

                    // clip source rectangle
                    Rect src = clipTile(t, pyr);

                    dst.right = dst.left + (int) (src.width() * scx);
                    dst.top = dst.bottom - (int) (src.height() * scy);

                    // load the bitmap
                    Bitmap img = bitmap(t);
                    canvas.drawBitmap(img, src, dst, p);

                    dst.bottom = dst.top;
                    //img.recycle();
                }

                dst.left = dst.right;
            }
        }
        catch(IOException e) {
            LOG.error("Error querying layer " + data.getName(), e);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        tx.apply(canvas);
    }

    Rect clipTile(Tile t, TilePyramid pyr) {
        Envelope tb = pyr.bounds(t);
        Envelope i = tb.intersection(view.getBounds());

        Rect rect = new Rect(0, 0, pyr.getTileWidth(), pyr.getTileHeight());

        int w = rect.width();
        int h = rect.height();

        rect.left += (i.getMinX() - tb.getMinX())/tb.getWidth() * w;
        rect.right -= (tb.getMaxX() - i.getMaxX())/tb.getWidth() * w;
        rect.top += (tb.getMaxY() - i.getMaxY())/tb.getHeight() * h; 
        rect.bottom -= (i.getMinY() - tb.getMinY())/tb.getHeight() * h;

        return rect;
    }

    @Override
    protected void drawBackground(RGB color) throws IOException {
        tx.reset(canvas);
        try {
            Paint p = paint(null, null);
            p.setStyle(Paint.Style.FILL);
            p.setColor(color(color));
            canvas.drawRect(new Rect(0, 0, view.getWidth(), view.getHeight()), p);
        }
        finally {
            tx.apply(canvas);
        }
    }

    void renderLabels() {
        for (Label l : labels.all()) {
            labeller.render(l);
        }
    }

    @Override
    protected void drawPoint(Feature f, Rule rule, Geometry point) {
        // markers drawn in pixel space
        tx.reset(canvas);

        float width = rule.number(f, MARKER_WIDTH, 10f);
        float height = rule.number(f, MARKER_HEIGHT, width);

        Paint fillPaint = markFillPaint(f, rule);
        Paint linePaint = markLinePaint(f, rule);
        
        if (fillPaint != null) {
            CoordinatePath path = 
                CoordinatePath.create(point).generalize(view.iscaleX(),view.iscaleY());
            while(path.hasNext()) {
                Coordinate c = path.next();
                canvas.drawOval(
                    rectFromCenter(tx.getWorldToCanvas().map(c), width, height), fillPaint);
            }
        }

        if (linePaint != null) {
            CoordinatePath path = 
                CoordinatePath.create(point).generalize(view.iscaleX(),view.iscaleY());
            while(path.hasNext()) {
                Coordinate c = path.next();
                canvas.drawOval(
                    rectFromCenter(tx.getWorldToCanvas().map(c), width, height), linePaint);
            }
        }

        // labels
        String label = rule.eval(f, TEXT_NAME, String.class);
        if (label != null) {
            createPointLabel(label, rule, f, point);
        }

        tx.apply(canvas);
    }

    void createPointLabel(String label, Rule rule, Feature f, Geometry g) {
        Label l = new Label(label, rule, f, g);

        Paint p = labelPaint(f, rule);
        l.put(Paint.class, p);

        labeller.layout(l, labels);
    }

    protected void drawLine(Feature f, Rule rule, Geometry line) {
        Path path = path(line);
        canvas.drawPath(path, linePaint(f, rule, tx.canvasToWorld));

        //labels
        String label = rule.eval(f, TEXT_NAME, String.class);
        if (label != null) {
            createLineLabel(label, rule, f, line);
        }
    }

    void createLineLabel(String label, Rule rule, Feature f, Geometry g) {
        Paint p = labelPaint(f, rule);

        LineLabel l = new LineLabel(label, rule, f, g);
        l.put(Paint.class, p);

        labeller.layout(l, labels);
    }

    protected void drawPolygon(Feature f, Rule rule, Geometry poly) {

        Paint fill = polyFillPaint(f, rule);
        Paint line = polyLinePaint(f, rule, tx.canvasToWorld);

        if (fill != null) {
            Path path = path(poly);
            canvas.drawPath(path, fill);
        }

        if (line != null) {
            Path path = path(poly);
            canvas.drawPath(path, line);
        }

        // labels
        String label = rule.eval(f, TEXT_NAME, String.class);
        if (label != null) {
            createPointLabel(label, rule, f, poly);
        }

        //drawPolygon(rp, buf, vpb.buffer(), color(polyFill), gamma, gammaMethod, color(lineColor), 
        //    lineWidth, lineGamma, lineGammaMethod, compOp);
    }

    Path path(Geometry g) {
        CoordinatePath cpath = 
            CoordinatePath.create(g).generalize(view.iscaleX(),view.iscaleY());
        return Graphics.path(cpath);
    }

    @Override
    protected void drawRasterRGBA(ByteBuffer raster, org.jeo.util.Rect pos, Rule rule) throws IOException {
        tx.reset(canvas);
        try {
            Bitmap bitmap = Bitmap.createBitmap(pos.width(), pos.height(), Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(raster);
            canvas.drawBitmap(bitmap, new Rect(0, 0, pos.width(), pos.height()), rect(pos), paint(null, rule));
        }
        finally {
            tx.apply(canvas);
        }
    }

    @Override
    protected void drawRasterGray(ByteBuffer raster, org.jeo.util.Rect pos, Rule rule) throws IOException {
        tx.reset(canvas);
        try {
            Bitmap bitmap = Bitmap.createBitmap(pos.width(), pos.height(), Bitmap.Config.ALPHA_8);
            bitmap.copyPixelsFromBuffer(raster);
            canvas.drawBitmap(bitmap, new Rect(0,0,pos.width(),pos.height()), rect(pos), paint(null, rule));
        }
        finally {
            tx.apply(canvas);
        }
    }

    Rect rect(org.jeo.util.Rect r) {
        return new Rect(r.left, r.top, r.right, r.bottom);
    }

    @Override
    protected void onFinish() throws IOException {
        if (bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, output);
        }
    }

    @Override
    public void close() {
        if (bitmap != null) {
            bitmap.recycle();
        }
    }
}
