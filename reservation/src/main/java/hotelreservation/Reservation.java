package hotelreservation;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Reservation_table")
public class Reservation {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long hotelId;
    private String hotelName;
    private String hotelStatus;
    private String hotelType;
    private String hotelPeriod;
    private Float hotelPrice;
    private String memberName;

    @PostUpdate
    public void onPostUpdate(){
        ReservationCanceled reservationCanceled = new ReservationCanceled();
        BeanUtils.copyProperties(this, reservationCanceled);
        reservationCanceled.publishAfterCommit();

    }

    @PrePersist
    public void onPrePersist() throws Exception {
        hotelreservation.external.Hotel hotel = new hotelreservation.external.Hotel();
       
        System.out.print("#######hotelId="+hotel);
        //Hotel 서비스에서 Hotel의 상태를 가져옴
        hotel = ReservationApplication.applicationContext.getBean(hotelreservation.external.HotelService.class)
            .getHotelStatus(hotelId);

        // fallback hotel이 null 이므로 예외처리 수행
        if (hotel == null){ 
            throw new Exception("The hotel is not in a usable status.");
        }
        
        // 예약 가능상태 여부에 따라 처리
        if ("Available".equals(hotel.gethotelStatus())){
            this.sethotelName(hotel.getHotelName());
            this.sethotelPeriod(hotel.gethotelPeriod());
            this.sethotelPrice(hotel.gethotelPrice());
            this.sethotelType(hotel.gethotelType());
            this.sethotelStatus("Confirmed");
        } else {
            throw new Exception("The hotel is not in a usable status.");
        }


    }

    @PostPersist
    public void onPostPersist() throws Exception {

        ReservationRegistered reservationRegistered = new ReservationRegistered();
        BeanUtils.copyProperties(this, reservationRegistered);
        reservationRegistered.publishAfterCommit();

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long gethotelId() {
        return hotelId;
    }

    public void sethotelId(Long hotelId) {
        this.hotelId = hotelId;
    }
    public String gethotelName() {
        return hotelName;
    }

    public void sethotelName(String hotelName) {
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
    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }




}
