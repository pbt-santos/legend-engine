// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.external.format.arrow;

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.arrow.adapter.jdbc.JdbcFieldInfo;
import org.apache.arrow.adapter.jdbc.JdbcToArrowConfig;
import org.apache.arrow.adapter.jdbc.JdbcToArrowConfigBuilder;
import org.apache.arrow.adapter.jdbc.LegendArrowVectorIterator;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.finos.legend.engine.external.shared.runtime.write.ExternalFormatWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ArrowDataWriter extends ExternalFormatWriter implements AutoCloseable
{
    private final LegendArrowVectorIterator iterator;
    private final BufferAllocator allocator;

    public ArrowDataWriter(ResultSet resultSet) throws SQLException
    {

        HashMap<Integer, JdbcFieldInfo> map = new HashMap<Integer, JdbcFieldInfo>();

        this.allocator = new RootAllocator();
        JdbcToArrowConfig config = new JdbcToArrowConfigBuilder(allocator, Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT)).build();
        this.iterator = LegendArrowVectorIterator.create(resultSet, config);

    }

    @Override
    public void writeData(OutputStream outputStream) throws IOException
    {
        try
        {
            while (this.iterator.hasNext())
            {
                try (VectorSchemaRoot vector = iterator.next();
                     ArrowStreamWriter writer = new ArrowStreamWriter(vector, null, outputStream)
                )
                {
                    writer.start();
                    writer.writeBatch();
                }
            }
        }
        catch (Exception e)
        {
            this.iterator.close();
            throw e;
        }

    }

    @Override
    public void writeDataAsString(OutputStream outputStream) throws IOException
    {
        try
        {
            while (this.iterator.hasNext())
            {
                try (VectorSchemaRoot vector = iterator.next())
                {
                    outputStream.write(vector.contentToTSVString().getBytes(Charset.forName("UTF-8")));
                }

            }
            this.close();
        }
        catch (Exception e)
        {
            this.close();
            throw e;
        }
    }

    @Override
    public void close()
    {
        allocator.close();
    }
}

