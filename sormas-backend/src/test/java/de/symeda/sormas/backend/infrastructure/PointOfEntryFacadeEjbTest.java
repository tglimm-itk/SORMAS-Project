package de.symeda.sormas.backend.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.symeda.sormas.api.infrastructure.pointofentry.PointOfEntryCriteria;
import de.symeda.sormas.api.infrastructure.pointofentry.PointOfEntryDto;
import de.symeda.sormas.api.infrastructure.pointofentry.PointOfEntryFacade;
import de.symeda.sormas.backend.AbstractBeanTest;
import de.symeda.sormas.backend.infrastructure.district.District;
import de.symeda.sormas.backend.infrastructure.region.Region;

class PointOfEntryFacadeEjbTest extends AbstractBeanTest {

	@Test
	void testGetAllAfter() throws InterruptedException {

		Region region = creator.createRegion("region");
		District district = creator.createDistrict("district", region);
		creator.createPointOfEntry("pointOfEntry1", region, district);
		getPointOfEntryService().doFlush();
		Date date = new Date();
		List<PointOfEntryDto> results = getPointOfEntryFacade().getAllAfter(date);

		// List should be empty
		assertEquals(0, results.size());

		Thread.sleep(1); // delay to ignore known rounding issues in change date filter
		String pointOfEntryName = "pointOfEntry2";
		creator.createPointOfEntry(pointOfEntryName, region, district);
		results = getPointOfEntryFacade().getAllAfter(date);

		// List should have one entry
		assertEquals(1, results.size());

		assertEquals(pointOfEntryName, results.get(0).getName());
		assertEquals(district.getUuid(), results.get(0).getDistrict().getUuid());
		assertEquals(region.getUuid(), results.get(0).getRegion().getUuid());
	}

	@Test
	void testCount() {

		getPointOfEntryService().createConstantPointsOfEntry();
		PointOfEntryFacade pointOfEntryFacade = getPointOfEntryFacade();
		assertEquals(0, pointOfEntryFacade.count(null));
		assertEquals(0, pointOfEntryFacade.count(new PointOfEntryCriteria()));

		Region region = creator.createRegion("Region1");
		District district = creator.createDistrict("District1", region);

		creator.createPointOfEntry("poe1", region, district);
		assertEquals(1, pointOfEntryFacade.count(null));
		assertEquals(1, pointOfEntryFacade.count(new PointOfEntryCriteria()));

		creator.createPointOfEntry("poe2", region, district);
		assertEquals(2, pointOfEntryFacade.count(null));
		assertEquals(2, pointOfEntryFacade.count(new PointOfEntryCriteria()));

		assertEquals(1, pointOfEntryFacade.count(new PointOfEntryCriteria().nameLike("poe1")));
	}

}
