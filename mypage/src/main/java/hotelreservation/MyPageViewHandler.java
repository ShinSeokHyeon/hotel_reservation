package hotelreservation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import hotelreservation.config.kafka.KafkaProcessor;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MyPageViewHandler {


    @Autowired
    private MyPageRepository myPageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenReservationRegistered_then_CREATE_1 (@Payload ReservationRegistered reservationRegistered) {
        try {

            if (!reservationRegistered.validate()) return;

            // view 객체 생성
            MyPage myPage = new MyPage();
            // view 객체에 이벤트의 Value 를 set 함
            myPage.setId(reservationRegistered.getId());
            myPage.setMemberName(reservationRegistered.getMemberName());
            myPage.sethotelId(reservationRegistered.gethotelId());
            myPage.sethotelName(reservationRegistered.gethotelName());
            myPage.sethotelStatus(reservationRegistered.gethotelStatus());
            myPage.sethotelType(reservationRegistered.gethotelType());
            myPage.sethotelPeriod(reservationRegistered.gethotelPeriod());
            myPage.sethotelPrice(reservationRegistered.gethotelPrice());
            // view 레파지 토리에 save
            myPageRepository.save(myPage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenReservationCanceled_then_UPDATE_1(@Payload ReservationCanceled reservationCanceled) {
        try {
            if (!reservationCanceled.validate()) return;
                // view 객체 조회
            Optional<MyPage> myPageOptional = myPageRepository.findById(reservationCanceled.getId());
            if( myPageOptional.isPresent()) {
                MyPage myPage = myPageOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                    myPage.sethotelStatus(reservationCanceled.gethotelStatus());
                // view 레파지 토리에 save
                myPageRepository.save(myPage);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}