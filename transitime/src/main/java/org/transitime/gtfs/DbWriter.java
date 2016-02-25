/**
 * 
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
package org.transitime.gtfs;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitime.db.hibernate.HibernateUtils;
import org.transitime.db.structs.Agency;
import org.transitime.db.structs.Block;
import org.transitime.db.structs.Calendar;
import org.transitime.db.structs.CalendarDate;
import org.transitime.db.structs.FareAttribute;
import org.transitime.db.structs.FareRule;
import org.transitime.db.structs.Frequency;
import org.transitime.db.structs.Route;
import org.transitime.db.structs.Stop;
import org.transitime.db.structs.Transfer;
import org.transitime.db.structs.TravelTimesForTrip;
import org.transitime.db.structs.Trip;
import org.transitime.db.structs.TripPattern;
import org.transitime.utils.IntervalTimer;

/**
 * Writes the GTFS data contained in a GtfsData object to the database.
 * 
 * @author SkiBu Smith
 *
 */
public class DbWriter {

	private final GtfsData gtfsData;
	private int counter = 0;
	private static final Logger logger = LoggerFactory
			.getLogger(DbWriter.class);

	/********************** Member Functions **************************/

	public DbWriter(GtfsData gtfsData) {
		this.gtfsData = gtfsData;
	}
	
	/**
	 * Actually writes data to database (once transaction closed).
	 * Uses Hibernate batching so don't use as much memory.
	 * @param object
	 */
	private void writeObject(Session session, Object object) {
		session.saveOrUpdate(object);

		// Since can writing large amount of data should use Hibernate 
		// batching to make sure don't run out memory.
		counter++;
		if (counter % HibernateUtils.BATCH_SIZE == 0) {
			session.flush();
			session.clear();
		}
	}
	
	/**
	 * Goes through the collections in GtfsData and writes the objects
	 * to the database.
	 * 
	 * @param configRev
	 */
	private void actuallyWriteData(Session session, int configRev) {
		// Get rid of old data. Getting rid of trips, trip patterns, and blocks
		// is a bit complicated. Need to delete them in proper order because
		// of the foreign keys. Because appear to need to use plain SQL
		// to do so successfully (without reading in objects and then
		// deleting them, which takes too much time and memory). Therefore
		// deleting of this data is done here before writing the data.
		logger.info("Deleting old blocks and associated trips from rev {} of "
				+ "database...", configRev);
		Block.deleteFromRev(session, configRev);

		logger.info("Deleting old trips from rev {} of database...", 
				configRev);
		Trip.deleteFromRev(session, configRev);

		logger.info("Deleting old trip patterns from rev {} of database...", 
				configRev);
		TripPattern.deleteFromRev(session, configRev);
		
		// Get rid of travel times that are associated with the rev being 
		// deleted
		logger.info("Deleting old travel times from rev {} of database...", 
				configRev);
		TravelTimesForTrip.deleteFromRev(session, configRev);
		
		// Now write the data to the database.
		// First write the Blocks. This will also write the Trips, TripPatterns,
		// Paths, and TravelTimes since those all have been configured to be
		// cascade=CascadeType.SAVE_UPDATE .
		logger.info("Saving {} blocks (plus associated trips) to database...", 
				gtfsData.getBlocks().size());
		int c = 0;
		for (Block block : gtfsData.getBlocks()) {
			logger.info("Saving block #{} with blockId={} serviceId={} blockId={}",
					++c, block.getId(), block.getServiceId(), block.getId());
			writeObject(session, block);
		}
		
		logger.info("Saving routes to database...");
		Route.deleteFromRev(session, configRev);
		for (Route route : gtfsData.getRoutes()) {
			writeObject(session, route);
		}
		
		logger.info("Saving stops to database...");
		Stop.deleteFromRev(session, configRev);
		for (Stop stop : gtfsData.getStops()) {
			writeObject(session, stop);
		}
		
		logger.info("Saving agencies to database...");
		Agency.deleteFromRev(session, configRev);
		for (Agency agency : gtfsData.getAgencies()) {
			writeObject(session, agency);
		}

		logger.info("Saving calendars to database...");
		Calendar.deleteFromRev(session, configRev);
		for (Calendar calendar : gtfsData.getCalendars()) {
			writeObject(session, calendar);
		}
		
		logger.info("Saving calendar dates to database...");
		CalendarDate.deleteFromRev(session, configRev);
		for (CalendarDate calendarDate : gtfsData.getCalendarDates()) {
			writeObject(session, calendarDate);
		}
		
		logger.info("Saving fare rules to database...");
		FareRule.deleteFromRev(session, configRev);
		for (FareRule fareRule : gtfsData.getFareRules()) {
			writeObject(session, fareRule);
		}
		
		logger.info("Saving fare attributes to database...");
		FareAttribute.deleteFromRev(session, configRev);
		for (FareAttribute fareAttribute : gtfsData.getFareAttributes()) {
			writeObject(session, fareAttribute);
		}
		
		logger.info("Saving frequencies to database...");
		Frequency.deleteFromRev(session, configRev);
		for (Frequency frequency : gtfsData.getFrequencies()) {
			writeObject(session, frequency);
		}
		
		logger.info("Saving transfers to database...");
		Transfer.deleteFromRev(session, configRev);
		for (Transfer transfer : gtfsData.getTransfers()) {
			writeObject(session, transfer);
		}
		
		// Write out the ConfigRevision data
		writeObject(session, gtfsData.getConfigRevision());
	}
	
	/**
	 * Writes the data for the collections that are part of the GtfsData
	 * object passed in to the constructor.
     *
	 * @param session
	 * @param configRev So can delete old data for the rev
	 * @throws HibernateException when problem with database
	 */
	public void write(Session session, int configRev)
			throws HibernateException {
		// For logging how long things take
		IntervalTimer timer = new IntervalTimer();

		// Let user know what is going on
		logger.info("Writing GTFS data to database...");

		Transaction tx = session.beginTransaction();
		
		// Do the low-level processing
		try {
			actuallyWriteData(session, configRev);
			
			// Done writing data so commit it
			tx.commit();
		} catch (HibernateException e) {
			logger.error("Error writing GTFS configuration data to db.", e);
			throw e;
		} 

		// Let user know what is going on
		logger.info("Finished writing GTFS data to database . Took {} msec.",
				timer.elapsedMsec());		
	}
}
