/* 
 * (C) Copyright 2015 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */

package io.github.msdk.featuredetection.chromatogramtofeaturetable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.chromatograms.Chromatogram;
import io.github.msdk.datamodel.datapointstore.DataPointStore;
import io.github.msdk.datamodel.datapointstore.DataPointStoreFactory;
import io.github.msdk.datamodel.featuretables.ColumnName;
import io.github.msdk.datamodel.featuretables.FeatureTable;
import io.github.msdk.datamodel.featuretables.FeatureTableColumn;
import io.github.msdk.datamodel.featuretables.FeatureTableRow;
import io.github.msdk.datamodel.featuretables.Sample;
import io.github.msdk.datamodel.impl.MSDKObjectBuilder;
import io.github.msdk.datamodel.ionannotations.IonAnnotation;
import io.github.msdk.datamodel.rawdata.ChromatographyInfo;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import io.github.msdk.datamodel.rawdata.SeparationType;
import io.github.msdk.featuredetection.targeteddetection.TargetedDetectionMethod;
import io.github.msdk.io.rawdataimport.mzml.MzMLFileImportMethod;
import io.github.msdk.util.MZTolerance;
import io.github.msdk.util.RTTolerance;

public class ChromatogramToFeatureTableMethodTest {

	private static final String TEST_DATA_PATH = "src/test/resources/";

	@SuppressWarnings({ "unchecked", "null" })
	@Test
	public void testOrbitrap() throws MSDKException {

		// Create the data structures
		final DataPointStore dataStore = DataPointStoreFactory.getMemoryDataStore();

		// Import the file
		File inputFile = new File(TEST_DATA_PATH + "orbitrap_300-600mz.mzML");
		Assert.assertTrue("Cannot read test data", inputFile.canRead());
		MzMLFileImportMethod importer = new MzMLFileImportMethod(inputFile);
		RawDataFile rawFile = importer.execute();
		Assert.assertNotNull(rawFile);
		Assert.assertEquals(1.0, importer.getFinishedPercentage(), 0.0001);

		// Ion 1
		List<IonAnnotation> ionAnnotations = new ArrayList<IonAnnotation>();
		IonAnnotation ion1 = MSDKObjectBuilder.getSimpleIonAnnotation();
		ion1.setExpectedMz(332.56);
		ion1.setAnnotationId("Feature 332.56");
		ion1.setChromatographyInfo(MSDKObjectBuilder.getChromatographyInfo1D(SeparationType.LC, (float) 772.8));
		ionAnnotations.add(ion1);

		// Ion 2
		IonAnnotation ion2 = MSDKObjectBuilder.getSimpleIonAnnotation();
		ion2.setExpectedMz(508.004);
		ion2.setAnnotationId("Feature 508.004");
		ion2.setChromatographyInfo(MSDKObjectBuilder.getChromatographyInfo1D(SeparationType.LC, (float) 868.8));
		ionAnnotations.add(ion2);

		// Ion 3
		IonAnnotation ion3 = MSDKObjectBuilder.getSimpleIonAnnotation();
		ion3.setExpectedMz(362.102);
		ion3.setAnnotationId("Feature 362.102");
		ion3.setChromatographyInfo(MSDKObjectBuilder.getChromatographyInfo1D(SeparationType.LC, (float) 643.2));
		ionAnnotations.add(ion3);

		// Variables
		final MZTolerance mzTolerance = new MZTolerance(0.003, 5.0);
		final RTTolerance rtTolerance = new RTTolerance(0.2, false);
		final Double intensityTolerance = 0.10d;
		final Double noiseLevel = 5000d;

		TargetedDetectionMethod chromBuilder = new TargetedDetectionMethod(ionAnnotations, rawFile, dataStore,
				mzTolerance, rtTolerance, intensityTolerance, noiseLevel);
		final List<Chromatogram> detectedChromatograms = chromBuilder.execute();
		Assert.assertEquals(1.0, chromBuilder.getFinishedPercentage(), 0.0001);
		List<Chromatogram> chromatograms = chromBuilder.getResult();
		Assert.assertNotNull(chromatograms);
		Assert.assertEquals(3, chromatograms.size());

		// Create a new feature table
		FeatureTable featureTable = MSDKObjectBuilder.getFeatureTable("orbitrap_300-600mz", dataStore);
		Sample sample = MSDKObjectBuilder.getSimpleSample("orbitrap_300-600mz");

		ChromatogramToFeatureTableMethod tableBuilder = new ChromatogramToFeatureTableMethod(chromatograms,
				featureTable, sample);
		tableBuilder.execute();

		// Verify row 2
		Assert.assertEquals(3, featureTable.getRows().size());
		Assert.assertEquals(17, featureTable.getColumns().size());
		FeatureTableRow row = featureTable.getRows().get(1);

		// Common columns
		Assert.assertEquals(2, row.getId(), 0.000001);
		Assert.assertEquals(508.0034287396599, row.getMz(), 0.000001);
		FeatureTableColumn column = featureTable.getColumn("Chromatography Info", null);
		ChromatographyInfo chromatographyInfo = row.getData(column, ChromatographyInfo.class);
		Assert.assertEquals(14.481896, chromatographyInfo.getRetentionTime() / 60, 0.000001);

		// Sample specific columns
		column = featureTable.getColumn("Id", null);
		int id = (int) row.getData(column, Integer.class);
		Assert.assertEquals(id, 2);

		column = featureTable.getColumn(ColumnName.MZ.getName(), sample);
		double mz = row.getData(column, Double.class);
		Assert.assertEquals(508.0034287396599, mz, 0.000001);

		column = featureTable.getColumn(ColumnName.RT.getName(), sample);
		double rt = row.getData(column, Double.class);
		Assert.assertEquals(14.481896, rt / 60, 0.000001);

		column = featureTable.getColumn(ColumnName.RTSTART.getName(), sample);
		double rtStart = row.getData(column, Double.class);
		Assert.assertEquals(14.16822, rtStart / 60, 0.000001);

		column = featureTable.getColumn(ColumnName.RTEND.getName(), sample);
		double rtEnd = row.getData(column, Double.class);
		Assert.assertEquals(14.9408865, rtEnd / 60, 0.000001);

		column = featureTable.getColumn(ColumnName.DURATION.getName(), sample);
		double duration = row.getData(column, Double.class);
		Assert.assertEquals(46.3599853515625, duration, 0.000001);

		column = featureTable.getColumn(ColumnName.HEIGHT.getName(), sample);
		double height = row.getData(column, Double.class);
		Assert.assertEquals(6317753.0, height, 0.000001);

		column = featureTable.getColumn(ColumnName.AREA.getName(), sample);
		double area = row.getData(column, Double.class);
		Assert.assertEquals(8.4486608E7, area, 0.000001);

		column = featureTable.getColumn(ColumnName.NUMBEROFDATAPOINTS.getName(), sample);
		int datapoints = (int) row.getData(column, Integer.class);
		Assert.assertEquals(18, datapoints, 0.000001);

		column = featureTable.getColumn(ColumnName.FWHM.getName(), sample);
		double fwhm = row.getData(column, Double.class);
		Assert.assertEquals(0.19513144, fwhm / 60, 0.000001);

		column = featureTable.getColumn(ColumnName.TAILINGFACTOR.getName(), sample);
		double tf = row.getData(column, Double.class);
		Assert.assertEquals(1.3311329466690036, tf, 0.000001);
		
		column = featureTable.getColumn(ColumnName.ASYMMETRYFACTOR.getName(), sample);
		double af = row.getData(column, Double.class);
		Assert.assertEquals(1.7337219745465573, af, 0.000001);

	}

}
