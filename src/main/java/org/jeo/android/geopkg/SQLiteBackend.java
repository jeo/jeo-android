package org.jeo.android.geopkg;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.vividsolutions.jts.geom.Geometry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jeo.geopkg.Backend;
import org.jeo.geopkg.GeoPkgOpts;
import org.jeo.geopkg.geom.GeoPkgGeomReader;
import org.jeo.geopkg.geom.GeoPkgGeomWriter;
import org.jeo.util.Pair;

/**
 *
 * @author Ian Schneider <ischneider@boundlessgeo.com>
 */
public class SQLiteBackend extends Backend {
    
    final SQLiteDatabase db;

    SQLiteBackend(GeoPkgOpts opts) {
        this.db = SQLiteDatabase.openOrCreateDatabase(opts.getFile(), null);
    }

    @Override
    protected boolean canRunScripts() {
        return false;
    }
    
    @Override
    protected Session session() throws IOException {
        return new SQLiteSession();
    }

    static String parsePrimaryKeyColumn(String tableDef) {
        Matcher matcher = Pattern.compile("\"([^\"]+)\" INTEGER.*PRIMARY KEY").matcher(tableDef);
        return matcher.find() ? matcher.group(1) : null;
    }

    @Override
    protected List<Pair<String, Class>> getColumnInfo(String table) throws IOException {
        List<Pair<String, Class>> info = new ArrayList<Pair<String,Class>>();
        String sql = String.format("PRAGMA table_info(%s)", table);
        Cursor cursor = db.rawQuery(sql, null);
        try {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String type = cursor.getString(cursor.getColumnIndex("type"));
                info.add(Pair.of(name, (Class) dbTypes.fromName(type)));
            }
        } finally {
            cursor.close();
        }
        return info;
    }

    @Override
    protected void closeInternal(Object object) throws Exception {
        if (object instanceof Cursor) {
            ((Cursor) object).close();
        } else {
            throw new RuntimeException("unable to close : " + object);
        }
    }

    @Override
    public void close() throws IOException {
        db.close();
    }

    class SQLiteSession extends Backend.Session {
        GeoPkgGeomReader geomReader;
        Cursor cursor;

        @Override
        protected void addBatch(String sql) throws IOException {
            db.execSQL(sql);
        }

        @Override
        protected void executeBatch() throws IOException {
            // do nothing
        }

        @Override
        protected void executePrepared(String sql, Object... args) throws IOException {
            GeoPkgGeomWriter writer = new GeoPkgGeomWriter();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Geometry) {
                    args[i] = writer.write((Geometry) args[i]);
                }
            }
            log(sql, args);
            db.execSQL(sql, args);
        }

        @Override
        protected void execute(String sql) throws IOException {
            executePrepared(sql);
        }

        @Override
        protected SQLiteResults queryPrepared(String sql, Object... args) throws IOException {
            String[] selectionArgs = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                selectionArgs[i] = String.valueOf(args[i]);
            }
            log(sql, args);
            return new SQLiteResults(db.rawQuery(sql, selectionArgs));
        }

        @Override
        protected Results query(String sql) throws IOException {
            return queryPrepared(sql);
        }

        @Override
        protected void endTransaction(boolean complete) throws IOException {
            if (complete) {
                db.setTransactionSuccessful();
            }
            db.endTransaction();
        }

        @Override
        protected void beginTransaction() throws IOException {
            db.beginTransaction();
        }

        @Override
        protected List<String> getPrimaryKeys(String tableName) throws IOException {
            List<String> keys = new ArrayList<String>(3);
            String sql = "SELECT sql from sqlite_master where type=? and name=?";
            SQLiteResults rs = queryPrepared(sql, "table", tableName);
            try {
                if (rs.next()) {
                    String tableDef = rs.getString(0);
                    String pkName = parsePrimaryKeyColumn(tableDef);
                    if (pkName != null) {
                        keys.add(pkName);
                    }
                }
            } finally {
                rs.close();
            }
            return keys;
        }

    }

    class SQLiteResults extends Backend.Results {
        final Cursor cursor;
        final GeoPkgGeomReader geomReader = new GeoPkgGeomReader();

        SQLiteResults(Cursor cursor) {
            this.cursor = cursor;
        }

        @Override
        protected boolean next() throws IOException {
            return cursor.moveToNext();
        }

        @Override
        protected Object getObject(int idx, Class t) throws IOException {
            Object obj;
            if (Geometry.class.isAssignableFrom(t)) {
                obj = geomReader.read(cursor.getBlob(idx));
            } else if (Long.class.equals(t)) {
                obj = cursor.getLong(idx);
            } else if (Integer.class.equals(t)
                    || Short.class.equals(t) || Byte.class.equals(t)) {
                obj = cursor.getInt(idx);
            } else if (Double.class.equals(t) || Float.class.equals(t)) {
                obj = cursor.getDouble(idx);
            } else {
                obj = cursor.getString(idx);
            }
            return obj;
        }

        @Override
        protected long getLong(int idx) throws IOException {
            return cursor.getLong(idx);
        }

        @Override
        protected String getString(String col) throws IOException {
            return cursor.getString(cursor.getColumnIndex(col));
        }

        @Override
        protected byte[] getBytes(int i) throws IOException {
            return cursor.getBlob(i);
        }


        @Override
        protected String getString(int idx) throws IOException {
            return cursor.getString(idx);
        }

        @Override
        protected int getInt(int idx) throws IOException {
            return cursor.getInt(idx);
        }

        @Override
        protected double getDouble(int idx) throws IOException {
            return cursor.getDouble(idx);
        }

        @Override
        protected boolean getBoolean(int idx) throws IOException {
            return cursor.getInt(idx) != 0;
        }

        @Override
        protected void closeInternal() throws Exception {
            cursor.close();
        }
    }
}
