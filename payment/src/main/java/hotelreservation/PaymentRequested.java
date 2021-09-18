package hotelreservation;

public class PaymentRequested extends AbstractEvent {

    private Long id;
    private Long reservId;
    private Float hotelPrice;
    private String reservStatus;

    public PaymentRequested(){
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getReservId() {
        return reservId;
    }

    public void setReservId(Long reservId) {
        this.reservId = reservId;
    }
    public Float gethotelPrice() {
        return hotelPrice;
    }

    public void sethotelPrice(Float hotelPrice) {
        this.hotelPrice = hotelPrice;
    }
    public String getReservStatus() {
        return reservStatus;
    }

    public void setReservStatus(String reservStatus) {
        this.reservStatus = reservStatus;
    }
}
