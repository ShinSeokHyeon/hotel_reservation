package hotelreservation.external;

public class Hotel {

    private Long id;
    private String hotelName;
    private String hotelStatus;
    private String hotelType;
    private String hotelPeriod;
    private Float hotelPrice;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getHotelName() {
        return hotelName;
    }
    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }
    public String gethotelStatus() {
        return hotelStatus;
    }
    public void sethotelStatus(String hotelStatus) {
        this.hotelStatus = hotelStatus;
    }
    public String gethotelType() {
        return hotelType;
    }
    public void sethotelType(String hotelType) {
        this.hotelType = hotelType;
    }
    public String gethotelPeriod() {
        return hotelPeriod;
    }
    public void sethotelPeriod(String hotelPeriod) {
        this.hotelPeriod = hotelPeriod;
    }
    public Float gethotelPrice() {
        return hotelPrice;
    }
    public void sethotelPrice(Float hotelPrice) {
        this.hotelPrice = hotelPrice;
    }

}
