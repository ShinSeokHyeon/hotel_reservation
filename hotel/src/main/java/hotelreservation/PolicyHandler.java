package hotelreservation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import hotelreservation.config.kafka.KafkaProcessor;

@Service
public class PolicyHandler{
    @Autowired HotelRepository hotelRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReservationCanceled_HotelStatusChangePolicy(@Payload ReservationCanceled reservationCanceled){

        if(!reservationCanceled.validate()) return;

        System.out.println("\n\n##### listener HotelStatusChangePolicy : " + reservationCanceled.toJson() + "\n\n");

        // 호텔 상태를 예약 가능 상태로 변경
        hotelRepository.findById(reservationCanceled.gethotelId())
            .ifPresent(
                hotel -> {
                	hotel.sethotelStatus("Available");
                    hotelRepository.save(hotel);
            }
        )
        ; 
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReservationRegistered_HotelStatusChangePolicy(@Payload ReservationRegistered reservationRegistered){

        if(!reservationRegistered.validate()) return;

        System.out.println("\n\n##### listener HotelStatusChangePolicy : " + reservationRegistered.toJson() + "\n\n");

        // 호텔 상태를 예약 불가능 상태로 변경
        hotelRepository.findById(reservationRegistered.gethotelId())
            .ifPresent(
            	hotel -> {
            		hotel.sethotelStatus("Not Available");
                    hotelRepository.save(hotel);
            }
        )
        ;    
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
