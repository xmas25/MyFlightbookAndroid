/*
	MyFlightbook for Android - provides native access to MyFlightbook
	pilot's logbook
    Copyright (C) 2017 MyFlightbook, LLC

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package Model;

public final class Solar {
    /// <summary>
    /// Calculate the UTC Time of sunrise for the given day at the given location on earth
    /// </summary>
    /// <param name="JD">Julian day</param>
    /// <param name="latitude">Latitude of observer</param>
    /// <param name="longitude">Longitude of observer</param>
    /// <returns>Time in minutes from zero Z</returns>
    static public double calcSunriseUTC(double JD, double latitude, double longitude)
    {
        double t = calcTimeJulianCent(JD);

        // *** Find the time of solar noon at the location, and use
        //     that declination. This is better than start of the 
        //     Julian day
        double noonmin = calcSolNoonUTC(t, longitude);
        double tnoon = calcTimeJulianCent(JD + noonmin / 1440.0);

        // *** First pass to approximate sunrise (using solar noon)
        double eqTime = calcEquationOfTime(tnoon);
        double solarDec = calcSunDeclination(tnoon);
        double hourAngle = calcHourAngleSunrise(latitude, solarDec);

        double delta = longitude - radToDeg(hourAngle);
        double timeDiff = 4 * delta;	// in minutes of time
        double timeUTC = 720 + timeDiff - eqTime;	// in minutes

        // *** Second pass includes fractional jday in gamma calc
        double newt = calcTimeJulianCent(calcJDFromJulianCent(t) + timeUTC / 1440.0);
        eqTime = calcEquationOfTime(newt);
        solarDec = calcSunDeclination(newt);
        hourAngle = calcHourAngleSunrise(latitude, solarDec);
        delta = longitude - radToDeg(hourAngle);
        timeDiff = 4 * delta;
        timeUTC = 720 + timeDiff - eqTime; // in minutes

        return timeUTC;
    }

    /// <summary>
    /// calculate the Universal Coordinated Time (UTC) of sunset for the given day at the given location on earth
    /// </summary>
    /// <param name="JD">Julian Day</param>
    /// <param name="latitude">latitude of observer in degrees</param>
    /// <param name="longitude">longitude of observer in degrees</param>
    /// <returns>time in minutes from zero Z</returns>
    static public double calcSunsetUTC(double JD, double latitude, double longitude)
    {
        double t = calcTimeJulianCent(JD);

        // *** Find the time of solar noon at the location, and use
        //     that declination. This is better than start of the 
        //     Julian day
        double noonmin = calcSolNoonUTC(t, longitude);
        double tnoon = calcTimeJulianCent(JD + noonmin / 1440.0);

        // First calculates sunrise and approx length of day
        double eqTime = calcEquationOfTime(tnoon);
        double solarDec = calcSunDeclination(tnoon);
        double hourAngle = calcHourAngleSunset(latitude, solarDec);

        double delta = longitude - radToDeg(hourAngle);
        double timeDiff = 4 * delta;
        double timeUTC = 720 + timeDiff - eqTime;

        // first pass used to include fractional day in gamma calc
        double newt = calcTimeJulianCent(calcJDFromJulianCent(t) + timeUTC / 1440.0);
        eqTime = calcEquationOfTime(newt);
        solarDec = calcSunDeclination(newt);
        hourAngle = calcHourAngleSunset(latitude, solarDec);

        delta = longitude - radToDeg(hourAngle);
        timeDiff = 4 * delta;
        timeUTC = 720 + timeDiff - eqTime; // in minutes

        return timeUTC;
    }

    /// <summary>
    /// calculate the hour angle of the sun at sunrise for the latitude
    /// </summary>
    /// <param name="lat">Latitude of observer in degrees</param>
    /// <param name="solarDec">declination angle of sun in degrees</param>
    /// <returns>hour angle of sunrise in radians</returns>
    static public double calcHourAngleSunrise(double lat, double solarDec)
    {
        double latRad = degToRad(lat);
        double sdRad = degToRad(solarDec);

        double HA = (Math.acos(Math.cos(degToRad(90.833)) / (Math.cos(latRad) * Math.cos(sdRad)) - Math.tan(latRad) * Math.tan(sdRad)));

        return HA;		// in radians
    }

    /// <summary>
    /// calculate the hour angle of the sun at sunset for the latitude
    /// </summary>
    /// <param name="lat">latitude of observer in degrees</param>
    /// <param name="solarDec">declination angle of sun in degrees</param>
    /// <returns>hour angle of sunset in radians</returns>
    static public double calcHourAngleSunset(double lat, double solarDec)
    {
        double latRad = degToRad(lat);
        double sdRad = degToRad(solarDec);

        // double HAarg = (Math.cos(degToRad(90.833)) / (Math.cos(latRad) * Math.cos(sdRad)) - Math.tan(latRad) * Math.tan(sdRad));

        double HA = (Math.acos(Math.cos(degToRad(90.833)) / (Math.cos(latRad) * Math.cos(sdRad)) - Math.tan(latRad) * Math.tan(sdRad)));

        return -HA;		// in radians
    }
    
    /// <summary>
    /// Solar angle - reverse engineered from http://www.usc.edu/dept-00/dept/architecture/mbs/tools/thermal/sun_calc.html
    /// </summary>
    /// <param name="lat">The Latitude, in degrees</param>
    /// <param name="lon">The Longitude, in degrees</param>
    /// <param name="minutes">Minutes into the day (UTC)</param>
    /// <param name="JD">The Julian Date</param>
    /// <returns>Angle of the sun, in degrees</returns>
    static public double calcSolarAngle(double lat, double lon, double JD, double minutes)
    {
        double t = calcTimeJulianCent(JD);
        double decl = degToRad(calcSunDeclination(t));
        double latRad = degToRad(lat);
        double altitudeAngle = 0.0;

        double solarMinutesAfterMidnight = minutes + (4 * lon);
        double hourAngle = (solarMinutesAfterMidnight - 12 * 60) / 4 * -1;

        altitudeAngle = Math.asin(
                    (Math.cos(latRad) * Math.cos(decl) * Math.cos(degToRad(hourAngle)) +
                    (Math.sin(latRad) * Math.sin(decl))));

        return radToDeg(altitudeAngle);
    }



    /// <summary>
    /// calculate the Universal Coordinated Time (UTC) of solar noon for the given day at the given location on earth
    /// </summary>
    /// <param name="t">number of Julian centuries since J2000.0</param>
    /// <param name="longitude">longitude of observer in degrees</param>
    /// <returns>time in minutes from zero Z</returns>
    public static double calcSolNoonUTC(double t, double longitude)
    {
        // First pass uses approximate solar noon to calculate eqtime
        double tnoon = calcTimeJulianCent(calcJDFromJulianCent(t) + longitude / 360.0);
        double eqTime = calcEquationOfTime(tnoon);
        double solNoonUTC = 720 + (longitude * 4) - eqTime; // min

        double newt = calcTimeJulianCent(calcJDFromJulianCent(t) - 0.5 + solNoonUTC / 1440.0);

        eqTime = calcEquationOfTime(newt);
        // double solarNoonDec = calcSunDeclination(newt);
        solNoonUTC = 720 + (longitude * 4) - eqTime; // min

        return solNoonUTC;
    }

    //***********************************************************************/
    //***********************************************************************/
    //*												*/
    //*This section contains subroutines used in calculating solar position */
    //*												*/
    //***********************************************************************/
    //***********************************************************************/
    /// <summary>
    /// Convert radian angle to degrees
    /// </summary>
    /// <param name="angleRad">Angle, in radians</param>
    /// <returns>Angle, in degres</returns>
    static public double radToDeg(double angleRad)
    {
        return (180.0 * angleRad / Math.PI);
    }

    // Convert degree angle to radians
    /// <summary>
    /// Convert degrees to radians
    /// </summary>
    /// <param name="angleDeg">Angle, in degrees</param>
    /// <returns>Angle, in Radians</returns>
    static public double degToRad(double angleDeg)
    {
        return (Math.PI * angleDeg / 180.0);
    }

    /// <summary>
    /// Julian day from calendar day
    /// </summary>
    /// <param name="year">Year</param>
    /// <param name="month">Month (1-12)</param>
    /// <param name="day">Day (1-31)</param>
    /// <returns>The Julian day corresponding to the date.  Number is returned for start of day.  Fractional days should be added later.</returns>
    static public double calcJD(int year, int month, int day)
    {
        if (month <= 2)
        {
            year -= 1;
            month += 12;
        }
        int A = (int)Math.floor(year / 100.0);
        int B = 2 - A + (int)Math.floor(A / 4.0);

        double JD = Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + B - 1524.5;
        return JD;
    }

    /// <summary>
    /// Convert Julian Day to centuries since J2000.0
    /// </summary>
    /// <param name="jd">Julian day to convert</param>
    /// <returns>The T value corresponding to the Julian Day</returns>
    public static double calcTimeJulianCent(double jd)
    {
        double T = (jd - 2451545.0) / 36525.0;
        return T;
    }

    /// <summary>
    /// convert centuries since J2000.0 to Julian Day
    /// </summary>
    /// <param name="t">number of Julian centuries since J2000.0</param>
    /// <returns>the Julian Day corresponding to the t value</returns>
    public static double calcJDFromJulianCent(double t)
    {
        double JD = t * 36525.0 + 2451545.0;
        return JD;
    }

    /// <summary>
    /// calculate the Geometric Mean Longitude of the Sun
    /// </summary>
    /// <param name="t">number of Julian centuries since J2000.0</param>
    /// <returns>the Geometric Mean Longitude of the Sun in degrees</returns>
    static double calcGeomMeanLongSun(double t)
    {
        double L0 = 280.46646 + t * (36000.76983 + 0.0003032 * t);
        while (L0 > 360.0)
            L0 -= 360.0;
        while (L0 < 0.0)
            L0 += 360.0;
        return L0;		// in degrees
    }

    /// <summary>
    /// calculate the Geometric Mean Anomaly of the Sun
    /// </summary>
    /// <param name="t">number of Julian centuries since J2000.0</param>
    /// <returns>the Geometric Mean Anomaly of the Sun in degrees</returns>
    static double calcGeomMeanAnomalySun(double t)
    {
        double M = 357.52911 + t * (35999.05029 - 0.0001537 * t);
        return M;		// in degrees
    }

    /// <summary>
    /// calculate the eccentricity of earth's orbit
    /// </summary>
    /// <param name="t">number of Julian centuries since J2000.0</param>
    /// <returns>the unitless eccentricity</returns>
    static double calcEccentricityEarthOrbit(double t)
    {
        double e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
        return e;		// unitless
    }

    /// <summary>
    /// calculate the equation of center for the sun
    /// </summary>
    /// <param name="t">number of Julian centuries since J2000.0</param>
    /// <returns>in degrees</returns>
    static double calcSunEqOfCenter(double t)
    {
        double m = calcGeomMeanAnomalySun(t);

        double mrad = degToRad(m);
        double sinm = Math.sin(mrad);
        double sin2m = Math.sin(mrad + mrad);
        double sin3m = Math.sin(mrad + mrad + mrad);

        double C = sinm * (1.914602 - t * (0.004817 + 0.000014 * t)) + sin2m * (0.019993 - 0.000101 * t) + sin3m * 0.000289;
        return C;		// in degrees
    }

    /// <summary>
    /// calculate the true longitude of the sun
    /// </summary>
    /// <param name="t">number of Julian centuries since J2000.0</param>
    /// <returns>sun's true longitude in degrees</returns>
    static double calcSunTrueLong(double t)
    {
        double l0 = calcGeomMeanLongSun(t);
        double c = calcSunEqOfCenter(t);

        double O = l0 + c;
        return O;		// in degrees
    }

    /// <summary>
    /// calculate the true anamoly of the sun
    /// </summary>
    /// <param name="t">number of Julian centuries since J2000.</param>
    /// <returns>sun's true anamoly in degrees</returns>
    static double calcSunTrueAnomaly(double t)
    {
        double m = calcGeomMeanAnomalySun(t);
        double c = calcSunEqOfCenter(t);

        double v = m + c;
        return v;		// in degrees
    }

    /// <summary>
    /// calculate the distance to the sun in AU
    /// </summary>
    /// <param name="t"> t : number of Julian centuries since J2000.0</param>
    /// <returns>sun radius vector in AUs</returns>
    static double calcSunRadVector(double t)
    {
        double v = calcSunTrueAnomaly(t);
        double e = calcEccentricityEarthOrbit(t);

        double R = (1.000001018 * (1 - e * e)) / (1 + e * Math.cos(degToRad(v)));
        return R;		// in AUs
    }

    /// <summary>
    /// calculate the apparent longitude of the sun
    /// </summary>
    /// <param name="t"> t : number of Julian centuries since J2000.0</param>
    /// <returns>sun's apparent longitude in degrees</returns>
    static double calcSunApparentLong(double t)
    {
        double o = calcSunTrueLong(t);

        double omega = 125.04 - 1934.136 * t;
        double lambda = o - 0.00569 - 0.00478 * Math.sin(degToRad(omega));
        return lambda;		// in degrees
    }

    /// <summary>
    /// calculate the mean obliquity of the ecliptic
    /// </summary>
    /// <param name="t"> t : number of Julian centuries since J2000.0</param>
    /// <returns>mean obliquity in degrees</returns>
    static double calcMeanObliquityOfEcliptic(double t)
    {
        double seconds = 21.448 - t * (46.8150 + t * (0.00059 - t * (0.001813)));
        double e0 = 23.0 + (26.0 + (seconds / 60.0)) / 60.0;
        return e0;		// in degrees
    }

    /// <summary>
    /// calculate the corrected obliquity of the ecliptic
    /// </summary>
    /// <param name="t"> t : number of Julian centuries since J2000.0</param>
    /// <returns>corrected obliquity in degrees</returns>
    static double calcObliquityCorrection(double t)
    {
        double e0 = calcMeanObliquityOfEcliptic(t);

        double omega = 125.04 - 1934.136 * t;
        double e = e0 + 0.00256 * Math.cos(degToRad(omega));
        return e;		// in degrees
    }

    /// <summary>
    /// calculate the right ascension of the sun
    /// </summary>
    /// <param name="t"> t : number of Julian centuries since J2000.0</param>
    /// <returns>sun's right ascension in degrees</returns>
    static double calcSunRtAscension(double t)
    {
        double e = calcObliquityCorrection(t);
        double lambda = calcSunApparentLong(t);

        double tananum = (Math.cos(degToRad(e)) * Math.sin(degToRad(lambda)));
        double tanadenom = (Math.cos(degToRad(lambda)));
        double alpha = radToDeg(Math.atan2(tananum, tanadenom));
        return alpha;		// in degrees
    }

    /// <summary>
    /// calculate the declination of the sun
    /// </summary>
    /// <param name="t"> t : number of Julian centuries since J2000.0</param>
    /// <returns>sun's declination in degrees</returns>
    static double calcSunDeclination(double t)
    {
        double e = calcObliquityCorrection(t);
        double lambda = calcSunApparentLong(t);

        double sint = Math.sin(degToRad(e)) * Math.sin(degToRad(lambda));
        double theta = radToDeg(Math.asin(sint));
        return theta;		// in degrees
    }

    /// <summary>
    /// calculate the difference between true solar time and mean solar time
    /// </summary>
    /// <param name="t">number of Julian centuries since J2000.0</param>
    /// <returns>equation of time in minutes of time</returns>
    static double calcEquationOfTime(double t)
    {
        double epsilon = calcObliquityCorrection(t);
        double l0 = calcGeomMeanLongSun(t);
        double e = calcEccentricityEarthOrbit(t);
        double m = calcGeomMeanAnomalySun(t);

        double y = Math.tan(degToRad(epsilon) / 2.0);
        y *= y;

        double sin2l0 = Math.sin(2.0 * degToRad(l0));
        double sinm = Math.sin(degToRad(m));
        double cos2l0 = Math.cos(2.0 * degToRad(l0));
        double sin4l0 = Math.sin(4.0 * degToRad(l0));
        double sin2m = Math.sin(2.0 * degToRad(m));

        double Etime = y * sin2l0 - 2.0 * e * sinm + 4.0 * e * y * sinm * cos2l0
                - 0.5 * y * y * sin4l0 - 1.25 * e * e * sin2m;

        return radToDeg(Etime) * 4.0;	// in minutes of time
    }
}
