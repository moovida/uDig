/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2012, Refractions Research Inc.
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package net.refractions.udig.project.internal.commands.edit;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.util.Iterator;

import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.tests.support.MapTests;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class AddFeatureCommandTest {

    /*
     * Test method for 'net.refractions.udig.project.internal.commands.edit.AddFeatureCommand.run(IProgressMonitor)'
     */
    @Test
    public void testRun() throws Exception {
        Map map = MapTests.createDefaultMap("test", 2, true, new Dimension(10,10)); //$NON-NLS-1$
        Layer layer = map.getLayersInternal().get(0);
        SimpleFeatureType schema = layer.getSchema();
		SimpleFeature feature = SimpleFeatureBuilder.build(schema, new Object[]{null,null}, "id");
        
        AddFeatureCommand command=new AddFeatureCommand(feature, layer);
        
        command.setMap(map);
        command.run(new NullProgressMonitor());
        FeatureSource<SimpleFeatureType, SimpleFeature> source = layer.getResource(FeatureSource.class, new NullProgressMonitor());
        assertEquals(3, source.getCount(Query.ALL));
        
        command.rollback(new NullProgressMonitor());
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
        int i=0;
        for( Iterator iter = collection.iterator(); iter.hasNext(); ) {
            iter.next();
            i++;
        }
        
        assertEquals(2, i);
        
    }

}
