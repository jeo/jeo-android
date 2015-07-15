package io.jeo.android.graphics;

/* Copyright 2014 The jeo project. All rights reserved.
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import io.jeo.map.View;
import io.jeo.render.RendererFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Android 2D renderer.
 */
public class Android2D implements RendererFactory<AndroidRenderer> {

    @Override
    public String getName() {
        return "Android";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("android2d");
    }

    @Override
    public List<String> getFormats() {
        return Arrays.asList("png", "image/png");
    }

    @Override
    public AndroidRenderer create(View view, Map<?, Object> opts) {
        Bitmap img = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        return new AndroidRenderer(new Canvas(img), img);
    }
}
