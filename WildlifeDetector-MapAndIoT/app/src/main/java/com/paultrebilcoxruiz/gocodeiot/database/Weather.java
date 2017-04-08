package com.paultrebilcoxruiz.gocodeiot.database;

public class Weather {

    private long time;
    private double temperature;
    private double dewpoint;
    private int relhumidity;
    private double pressure;
    private int uv;
    private int airquality;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getDewpoint() {
        return dewpoint;
    }

    public void setDewpoint(double dewpoint) {
        this.dewpoint = dewpoint;
    }

    public int getRelhumidity() {
        return relhumidity;
    }

    public void setRelhumidity(int relhumidity) {
        this.relhumidity = relhumidity;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public int getUv() {
        return uv;
    }

    public void setUv(int uv) {
        this.uv = uv;
    }

    public int getAirquality() {
        return airquality;
    }

    public void setAirquality(int airquality) {
        this.airquality = airquality;
    }

    public String toString() {
        return "Weather: " +
                "\ntemp: " + temperature +
                "\nhumidity: " + relhumidity +
                "\npressure: " + pressure +
                "\nuv: " + uv +
                "\nair quality: " + airquality +
                "\ndew point: " + dewpoint;
    }
}
