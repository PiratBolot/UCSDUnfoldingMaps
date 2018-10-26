package module5;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import parsing.ParseFeed;
import processing.core.PApplet;
import processing.core.PGraphics;

/** EarthquakeCityMap
 * An application with an interactive map displaying earthquake data.
 * Author: UC San Diego Intermediate Software Development MOOC team
 * @author Kapustin Roman
 * */
public class EarthquakeCityMap extends PApplet {
	
	// You can ignore this. It's to get rid of eclipse warnings
	private static final long serialVersionUID = 1L;

	// IF YOU ARE WORKING OFFILINE, change the value of this variable to true
	private static final boolean OFFLINE = false;
	
	/** This is where to find the local tiles, for working without an Internet connection */
	public static String mbTilesString = "blankLight-1-3.mbtiles";
	
	//feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";
	
	// The files containing city names and info and country names and info
	private String cityFile = "./data/city-data.json";
	private String countryFile = "./data/countries.geo.json";

	private PGraphics buffer;

	private UnfoldingMap map;
	
	// Markers for each city
	private List<Marker> cityMarkers;

	// Markers for each earthquake
	private List<Marker> quakeMarkers;

	// A List of country markers
	private List<Marker> countryMarkers;
	
	// NEW IN MODULE 5
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;

    static {
        System.setProperty("user.dir", "./Projects/Coursera/Java/UCSDUnfoldingMaps");
    }

	@Override
	public void setup() {
		// (1) Initializing canvas and map tiles
		size(900, 700, OPENGL);
		if (OFFLINE) {
		    map = new UnfoldingMap(this, 200, 50, 650, 600, new MBTilesMapProvider(mbTilesString));
		    earthquakesURL = "2.5_week.atom";  // The same feed, but saved August 7, 2015
		}
		else {
			map = new UnfoldingMap(this, 200, 50, 650, 600, new Microsoft.RoadProvider());
		}
		MapUtils.createDefaultEventDispatcher(this, map);

		// (2) Reading in earthquake data and geometric properties
	    //     STEP 1: load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);

		//     STEP 2: read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for(Feature city : cities) {
		  cityMarkers.add(new CityMarker(city));
		}
	    
		//     STEP 3: read in earthquake RSS feed
	    List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
	    quakeMarkers = new ArrayList<Marker>();
	    
	    for(PointFeature feature : earthquakes) {
		  //check if LandQuake
		  if(isLand(feature)) {
		    quakeMarkers.add(new LandQuakeMarker(feature));
		  }
		  // OceanQuakes
		  else {
		    quakeMarkers.add(new OceanQuakeMarker(feature));
		  }
	    }
	    // could be used for debugging
//	    printQuakes();

	    map.addMarkers(quakeMarkers);
	    map.addMarkers(cityMarkers);
		this.buffer = createGraphics(900, 700);
	}
	
	@Override
	public void draw() {
		background(0);
		map.draw();
		addKey();
		if (lastSelected != null) {
			g.beginDraw();
			lastSelected.showTitle(g, mouseX, mouseY);
			g.endDraw();
//			image(buffer.get(), mouseX, mouseY);
		}
	}
	
	/** Event handler that gets called automatically when the 
	 * mouse moves.
	 */
	@Override
	public void mouseMoved()
	{
		buffer.clear();
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;
		}
		if (!selectMarkerIfHover(quakeMarkers)) {
			selectMarkerIfHover(cityMarkers);
		}
	}
	
	// If there is a marker under the cursor, and lastSelected is null 
	// set the lastSelected to be the first marker found under the cursor
	// Make sure you do not select two markers.
	// 
	private boolean selectMarkerIfHover(List<Marker> markers)
	{
		for (Marker marker : markers) {
			if (marker.isInside(map, mouseX, mouseY)) {
				lastSelected = (CommonMarker)marker;
				lastSelected.setSelected(true);
				return true;
			}
		}
		return false;
	}
	
	/** The event handler for mouse clicks
	 * It will display an earthquake and its threat circle of cities
	 * Or if a city is clicked, it will display all the earthquakes 
	 * where the city is in the threat circle
	 */
	@Override
	public void mouseClicked()
	{
		if (lastClicked != null) {
//			noStroke();
			unhideMarkers();
			lastClicked.setClicked(false);
			lastClicked = null;
		} else if (lastSelected != null) {
			lastClicked = lastSelected;
			lastClicked.setClicked(true);
			if (lastSelected instanceof CityMarker) {
				List<EarthquakeMarker> list = quakeMarkers
						.stream()
						.map(elem -> (EarthquakeMarker)elem)
						.collect(Collectors.toList());
				for (EarthquakeMarker marker : list) {
					if (lastSelected.getDistanceTo(marker.getLocation()) > marker.threatCircle()) {
						marker.setHidden(true);
					}
				}
				for (Marker marker : cityMarkers) {
					if (lastSelected != marker) {
						marker.setHidden(true);
					}
				}
			} else {
				HashMap<String, Object> properties = lastClicked.getProperties();
				List<ScreenPosition> tc = new ArrayList<>();
				double thrCircle = ((EarthquakeMarker)(lastSelected)).threatCircle();
				for (Marker city : cityMarkers) {
					if (city.getDistanceTo(lastSelected.getLocation()) > thrCircle) {
						city.setHidden(true);
					} else {
						ScreenPosition sp = ((CityMarker) city).getScreenPosition(map);
						tc.add(sp);
					}
				}
				properties.put("threatedCities", tc);
				lastClicked.setProperties(properties);
				for (Marker quake : quakeMarkers) {
					if (lastSelected != quake) {
						quake.setHidden(true);
					}
				}
			}
		}
	}
	
	
	// loop over and unhide all markers
	private void unhideMarkers() {
		for(Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}
			
		for(Marker marker : cityMarkers) {
			marker.setHidden(false);
		}
	}
	
	// helper method to draw key in GUI
	private void addKey() {
		// Remember you can use Processing's graphics methods here
		fill(255, 250, 240);
		
		int xbase = 25;
		int ybase = 50;
		
		rect(xbase, ybase, 150, 250);
		
		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", xbase+25, ybase+25);
		
		fill(150, 30, 30);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase-CityMarker.TRI_SIZE, tri_xbase-CityMarker.TRI_SIZE, 
				tri_ybase+CityMarker.TRI_SIZE, tri_xbase+CityMarker.TRI_SIZE, 
				tri_ybase+CityMarker.TRI_SIZE);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("City Marker", tri_xbase + 15, tri_ybase);
		
		text("Land Quake", xbase+50, ybase+70);
		text("Ocean Quake", xbase+50, ybase+90);
		text("Size ~ Magnitude", xbase+25, ybase+110);
		
		fill(255, 255, 255);
		ellipse(xbase+35, 
				ybase+70, 
				10, 
				10);
		rect(xbase+35-5, ybase+90-5, 10, 10);
		
		fill(color(255, 255, 0));
		ellipse(xbase+35, ybase+140, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase+35, ybase+160, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase+35, ybase+180, 12, 12);
		
		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase+50, ybase+140);
		text("Intermediate", xbase+50, ybase+160);
		text("Deep", xbase+50, ybase+180);

		text("Past hour", xbase+50, ybase+200);
		
		fill(255, 255, 255);
		int centerx = xbase+35;
		int centery = ybase+200;
		ellipse(centerx, centery, 12, 12);

		strokeWeight(2);
		line(centerx-8, centery-8, centerx+8, centery+8);
		line(centerx-8, centery+8, centerx+8, centery-8);
	}

	
	
	// Checks whether this quake occurred on land.  If it did, it sets the 
	// "country" property of its PointFeature to the country where it occurred
	// and returns true.  Notice that the helper method isInCountry will
	// set this "country" property already.  Otherwise it returns false.	
	private boolean isLand(PointFeature earthquake) {

		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}
		
		// not inside any country
		return false;
	}
	
	// prints countries with number of earthquakes
	private void printQuakes() {
		HashMap<String, Integer> countryQuake = new HashMap<>();
		String country;

		for (Marker marker : quakeMarkers) {
			int count = 1;
			if (((EarthquakeMarker) marker).isOnLand()) {
				country = (String) marker.getProperty("country");
			} else {
				country = "OCEAN QUAKES";
			}
			if (countryQuake.containsKey(country)) {
				count = countryQuake.get(country) + 1;
			}
			countryQuake.put(country, count);
		}

		for (Map.Entry<String, Integer> entry : countryQuake.entrySet()) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}
	}
	
	
	
	// helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the earthquake feature if 
	// it's in one of the countries.
	// You should not have to modify this code
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if(country.getClass() == MultiMarker.class) {
				
			// looping over markers making up MultiMarker
			for(Marker marker : ((MultiMarker)country).getMarkers()) {
					
				// checking if inside
				if(((AbstractShapeMarker)marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));
						
					// return if is inside one
					return true;
				}
			}
		}
			
		// check if inside country represented by SimplePolygonMarker
		else if(((AbstractShapeMarker)country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));
			
			return true;
		}
		return false;
	}

	public CommonMarker getLastSelected() {
		return lastSelected;
	}

	public CommonMarker getLastClicked() {
		return lastClicked;
	}
}