package module5;

import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.SimplePointMarker;
import processing.core.PConstants;
import processing.core.PGraphics;

/** Implements a visual marker for cities on an earthquake map
 * 
 * @author UC San Diego Intermediate Software Development MOOC team
 * @author Kapustin Roman
 *
 */

public class CityMarker extends CommonMarker {
	
	public static int TRI_SIZE = 5;  // The size of the triangle marker
	
	public CityMarker(Location location) {
		super(location);
	}
	
	
	public CityMarker(Feature city) {
		super(((PointFeature)city).getLocation(), city.getProperties());
		// Cities have properties: "name" (city name), "country" (country name)
		// and "population" (population, in millions)
	}

	
	/**
	 * Implementation of method to draw marker on the map.
	 */
	@Override
	public void drawMarker(PGraphics pg, float x, float y) {
		// Save previous drawing style
		pg.pushStyle();

		pg.fill(150, 30, 30);
		pg.triangle(x, y - TRI_SIZE, x - TRI_SIZE, y + TRI_SIZE, x + TRI_SIZE,
                y + TRI_SIZE);
		
		// Restore previous drawing style
		pg.popStyle();
	}



	/** Show the title of the city if this marker is selected */
	public void showTitle(PGraphics pg, float x, float y)
	{
		if (!hidden) {
			String name = "Name: " + getCity();
			String country = "\nCountry: " + getCountry();
			String population = "\nPopulation: " + getPopulation();
			double maxLength = Math.max(Math.max(pg.textWidth(name), pg.textWidth(country)),
					pg.textWidth(population)) + 6;

			pg.fill(255, 255, 204);
			pg.rect(x - radius, y + 1.5f * radius, (float) maxLength + 1f, 60);
			pg.fill(0);
			pg.text(name + country + population, x - radius + 3, y + 1.5f * radius + 30);
		}
	}
	
	
	
	/* Local getters for some city properties.  
	 */
	public String getCity()
	{
		return getStringProperty("name");
	}
	
	public String getCountry()
	{
		return getStringProperty("country");
	}

	public float getPopulation()
	{
		return Float.parseFloat(getStringProperty("population"));
	}

	@Override
	public String toString() {
		return "CityMarker{" +
				"properties=" + properties +
				'}';
	}
}
