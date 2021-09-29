package hotelreservation;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

 @RestController
 public class HotelController {

	 	private HotelRepository repository;

	    public HotelController(HotelRepository repository){
	        this.repository = repository;
	    }
	    
	 // getResortStatus get 호출 시 400밀리초 ~ 620밀리초의 지연시간 발생시킴
	    @RequestMapping(method= RequestMethod.GET, value="/resorts/{id}")
	    public Hotel getHotelStatus(@PathVariable("id") Long id){

	        //hystix test code
	        try {
	              Thread.currentThread().sleep((long) (400 + Math.random() * 220)); // (+ 0~1*220)
	         } catch (InterruptedException e) { }

	        return repository.findById(id).get();
	    }


 }
