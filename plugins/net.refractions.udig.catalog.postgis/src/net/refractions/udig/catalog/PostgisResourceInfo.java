/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004, Refractions Research Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package net.refractions.udig.catalog;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import net.refractions.udig.catalog.IResolve.Status;
import net.refractions.udig.catalog.internal.postgis.PostgisPlugin;
import net.refractions.udig.catalog.postgis.internal.Messages;
import net.refractions.udig.ui.graphics.Glyph;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.DataSourceException;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

class PostgisResourceInfo extends IGeoResourceInfo {

	private PostgisGeoResource2 owner;

	PostgisResourceInfo(PostgisGeoResource2 owner) throws IOException {
		this.owner = owner;
		this.name = owner.typename;
		if(owner.desc.geometryType != null) {
			this.icon = Glyph.icon(owner.desc.geometryType);
		}

		keywords = new String[] { "teradata", //$NON-NLS-1$
				owner.typename };
	}

	@Override
	public synchronized ReferencedEnvelope getBounds() {
        if (bounds == null) {
            SimpleFeatureSource source = null;
            try {
                source = owner.resolve(SimpleFeatureSource.class, new NullProgressMonitor());
            } catch (IOException e) {
                PostgisPlugin.log("Could not establish bounds of dataset.", e); //$NON-NLS-1$
                bounds = new ReferencedEnvelope(new Envelope(), null);
                return bounds;
            }
            try {
                ReferencedEnvelope temp = source.getBounds();
                bounds = temp;
            } catch (Exception e) {
                PostgisPlugin.log("PostGIS unable to calculate bounds directly: "+e, e); //$NON-NLS-1$
            }
            if (bounds == null) {
                try {
                    CoordinateReferenceSystem crs = getCRS();
                    // try getting an envelope out of the crs
                    org.opengis.geometry.Envelope envelope = CRS.getEnvelope(crs);

                    if (envelope != null) {
                        bounds = new ReferencedEnvelope(new Envelope(envelope.getLowerCorner()
                                .getOrdinate(0), envelope.getUpperCorner().getOrdinate(0), envelope
                                .getLowerCorner().getOrdinate(1), envelope.getUpperCorner()
                                .getOrdinate(1)), crs);
                    } else {
                        // TODO: perhaps access a preference which indicates
                        // whether to do a full table scan
                        // bounds = new ReferencedEnvelope(new Envelope(),crs);
                        // as a last resort do the full scan
                        bounds = new ReferencedEnvelope(new Envelope(), crs);
                        FeatureIterator<SimpleFeature> iter = source.getFeatures().features();
                        try {
                            while (iter.hasNext()) {
                                SimpleFeature element = iter.next();
                                if (bounds.isNull())
                                    bounds.init(element.getBounds());
                                else
                                    bounds.include(element.getBounds());
                            }
                        } finally {
                            iter.close();
                        }
                    }
                } catch (DataSourceException e) {
                    PostgisPlugin.log("Could not establish bounds of dataset.", e); //$NON-NLS-1$
                } catch (Exception e) {
                    CatalogPlugin
                            .getDefault()
                            .getLog()
                            .log(new org.eclipse.core.runtime.Status(
                                    IStatus.WARNING,
                                    "net.refractions.udig.catalog", 0, Messages.PostGISGeoResource_error_layer_bounds, e)); //$NON-NLS-1$
                    bounds = new ReferencedEnvelope(new Envelope(), null);
                }
            }
        }
        return bounds;
    }

	public CoordinateReferenceSystem getCRS() {
		SimpleFeatureType ft = getFeatureType();
		if (ft == null) {
			return DefaultGeographicCRS.WGS84;
		}
		return ft.getCoordinateReferenceSystem();
	}

	private SimpleFeatureType getFeatureType() {
		try {
			return owner.getSchema();
		} catch (IOException e) {
			if (e.getMessage().contains("permission")) { //$NON-NLS-1$
				owner.setStatus(Status.RESTRICTED_ACCESS, e);
			} else {
				owner.setStatus(Status.BROKEN, e);
			}
			PostgisPlugin
					.log("Unable to retrieve SimpleFeatureType schema for type '" + owner.typename + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
			keywords = new String[] { "teradata", //$NON-NLS-1$
					owner.typename };
			return null;
		}

	}

	public String getName() {
		return owner.typename;
	}

	public URI getSchema() {
		SimpleFeatureType ft = getFeatureType();
		if (ft == null)
			return null;

		try {
			Name typeName = ft.getName();
			if (typeName.getNamespaceURI() != null) {
				return new URI(ft.getName().getNamespaceURI());
			} else {
				return null; // should probably be GML?
			}
		} catch (URISyntaxException e) {
			return null;
		}
	}

	public String getTitle() {
		return owner.typename;
	}
}