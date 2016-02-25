/*
 * This file is part of Transitime.org
 * 
 * Transitime.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL) as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Transitime.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transitime.org .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transitime.custom.lametro;

import org.transitime.avl.NextBusAvlModule;
import org.transitime.config.StringConfigValue;

/**
 * For lametro agency using two separate AVL feeds so need a 
 * separate agency name for feed for rail.
 *
 * @author SkiBu Smith
 * 
 */
public class LametroRailNextBusAvlModule extends NextBusAvlModule {

	private static StringConfigValue agencyNameForFeed =
			new StringConfigValue("transitime.custom.lametro.agencyNameForLametroRailFeed",
					"If set then specifies the agency name to use for the "
					+ "feed. If not set then the transitime.core.agencyId "
					+ "is used.");
	@Override
	protected String getAgencyNameForFeed() {
		return agencyNameForFeed.getValue();
	}

	/**
	 * @param agencyId
	 */
	public LametroRailNextBusAvlModule(String agencyId) {
		super(agencyId);
	}
	

}
