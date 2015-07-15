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
package io.jeo.android.geopkg;

import java.io.IOException;

import io.jeo.android.SQLiteBackend;
import io.jeo.geopkg.Backend;
import io.jeo.geopkg.GeoPkgBaseDriver;
import io.jeo.geopkg.GeoPkgOpts;

/**
 * Driver for the GeoPackage format, that utilizes Android SQLite capabilities. 
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoPackage extends GeoPkgBaseDriver {

    @Override
    protected Backend backend(GeoPkgOpts gpkgOpts) throws IOException {
        return new SQLiteBackend(gpkgOpts.getFile());
    }

}
