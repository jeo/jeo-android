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
package io.jeo.android.mbtiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.jeo.android.SQLiteBackend;
import io.jeo.mbtiles.MBTileSet;
import io.jeo.data.FileDriver;
import io.jeo.mbtiles.MBTilesOpts;

/**
 * Driver for the MBTiles format, that utilizes Android SQLite capabilities.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class MBTiles extends FileDriver<MBTileSet> {

    public static MBTileSet open(Path path){
        File file = path.toFile();
        return new MBTileSet(new SQLiteBackend(file), new MBTilesOpts(file));
    }

    @Override
    public String name() {
        return "MBTiles";
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("mbt");
    }

    @Override
    public Class<MBTileSet> type() {
        return MBTileSet.class;
    }

    @Override
    public MBTileSet open(File file, Map<?, Object> opts) throws IOException {
        return new MBTileSet(new SQLiteBackend(file), new MBTilesOpts(file));
    }

    @Override
    public Set<Capability> capabilities() {
        return Collections.emptySet();
    }
}
