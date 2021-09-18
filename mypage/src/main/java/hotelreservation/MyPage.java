package hotelreservation;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="MyPage_table")
public class MyPage {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private String memberName;
        private Long hotelId;
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
        public String getMemberName() {
            return memberName;
        }

        public void setMemberName(String memberName) {
            this.memberName = memberName;
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

}
