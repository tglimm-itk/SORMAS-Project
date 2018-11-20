/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.backend.region;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import de.symeda.sormas.backend.common.AbstractDomainObject;

@Entity
public class District extends AbstractDomainObject {
	
	private static final long serialVersionUID = -6057113756091470463L;

	public static final String TABLE_NAME = "district";
	
	public static final String NAME = "name";
	public static final String REGION = "region";
	public static final String EPID_CODE = "epidCode";
	public static final String COMMUNITIES = "communities";
	public static final String POPULATION = "population";
	public static final String GROWTH_RATE = "growthRate";
	
	private String name;
	private Region region;
	private String epidCode;
	private List<Community> communities;
	private Integer population;
	private Float growthRate;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@ManyToOne(cascade = CascadeType.REFRESH, optional = false)
	@JoinColumn(nullable = false)
	public Region getRegion() {
		return region;
	}
	public void setRegion(Region region) {
		this.region = region;
	}

	public String getEpidCode() {
		return epidCode;
	}
	public void setEpidCode(String epidCode) {
		this.epidCode = epidCode;
	}
	
	@OneToMany(mappedBy = Community.DISTRICT, cascade = {}, fetch = FetchType.LAZY)
	@OrderBy(District.NAME)
	public List<Community> getCommunities() {
		return communities;
	}
	public void setCommunities(List<Community> communities) {
		this.communities = communities;
	}
	
	public Integer getPopulation() {
		return population;
	}
	public void setPopulation(Integer population) {
		this.population = population;
	}
	
	public Float getGrowthRate() {
		return growthRate;
	}
	public void setGrowthRate(Float growthRate) {
		this.growthRate = growthRate;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
