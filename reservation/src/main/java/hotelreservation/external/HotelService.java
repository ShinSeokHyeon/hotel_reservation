
package hotelreservation.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;

@FeignClient(name="resort", url="${feign.resort.url}")
public interface HotelService {

    //@PatchMapping(path="/resorts/{id}")
    //@RequestMapping(method= RequestMethod.PATCH, path="/resorts/{id}")
    //public void resortStatusChange(@RequestParam("id") Long id, @RequestBody Resort resort);

    @RequestMapping(method= RequestMethod.GET, value="/hotels/{id}", consumes = "application/json")
    public Hotel getHotelStatus(@PathVariable("id") Long id);

}