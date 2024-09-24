package com.example.dncinema.controller;

import com.example.dncinema.dto.SeatPriceDTO;
import com.example.dncinema.dto.TicketDTO;
import com.example.dncinema.model.Customer;
import com.example.dncinema.model.Discount;
import com.example.dncinema.model.Film;
import com.example.dncinema.model.Seat;
import com.example.dncinema.repository.ICustomerRepository;
import com.example.dncinema.repository.IMovieRepository;
import com.example.dncinema.repository.seat.ISeatRepository;
import com.example.dncinema.service.ITicketServiceMinh;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/user/ticket")
@CrossOrigin("*")
public class TicketControllerMinh {
    @Autowired
    private ITicketServiceMinh iTicketServiceMinh;
    @Autowired
    private ISeatRepository iSeatRepository;
    @Autowired
    private ICustomerRepository iCustomerRepository;
    @Autowired
    private IMovieRepository iMovieRepository;

    /**
     * Get discount code from frond end and check, if exists will return discount object if not return 404 status
     * @author MinhNV
     * @param discount
     * @return object Discount
     * @since 27/04/2023
     */

    @GetMapping("/check-discount")
    public ResponseEntity<Discount> checkDiscount(@RequestParam(name = "nameDiscount") String discount) {
        Discount discount1 = iTicketServiceMinh.findDiscount(discount);
        if (discount1 == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(discount1, HttpStatus.OK);
    }

    /**
     * Get customer code from frond end and return object corresponding to that code
     * @author MinhNV
     * @param useName
     * @return object Customer and status
     * @since 27/04/2023
     */

    @GetMapping("/get-customer")
    public ResponseEntity<Customer> getCustomer(@RequestParam(name = "username") String useName) {
        return new ResponseEntity<>(iCustomerRepository.findByAccountUser_NameAccount(useName), HttpStatus.OK);
    }

    /**
     * Enter the necessary parameters to save the ticket information to the database
     * @author MinhNV
     * @param idCus
     * @param idFilm
     * @param codeDiscount
     * @param price
     * @param seat
     * @param vnp_ResponseCode
     * @return status 200 if exist else return status 404
     * @throws UnsupportedEncodingException
     * @since 27/05/2023
     */

     @GetMapping("/create")
     public ResponseEntity<?> saveTicket(
             @RequestParam(name = "idCus") String idCus,
             @RequestParam(name = "idFilm") String idFilm,
             @RequestParam(name = "idDiscount", required = false) String codeDiscount,
             @RequestParam(name = "price") String price,
             @RequestParam(name = "seat") String seat, // Ghế được phân tách bằng dấu phẩy
             @RequestParam(name = "vnp_ResponseCode") String vnp_ResponseCode
     ) throws UnsupportedEncodingException {
     
         // Kiểm tra các tham số không được rỗng hoặc null
         if (idCus == null || idFilm == null || price == null || seat == null ||
                 idCus.isEmpty() || idFilm.isEmpty() || price.isEmpty() || seat.isEmpty()) {
             return new ResponseEntity<>("Required parameters are missing.", HttpStatus.BAD_REQUEST);
         }
     
         // Tách các ghế từ chuỗi
         String[] list = seat.split(",");
         Integer[] listSeat = new Integer[list.length];
         for (int i = 0; i < list.length; i++) {
             try {
                 listSeat[i] = Integer.parseInt(list[i].trim());
             } catch (NumberFormatException e) {
                 return new ResponseEntity<>("Invalid seat number format.", HttpStatus.BAD_REQUEST);
             }
         }
     
         // Tạo đối tượng TicketDTO
         TicketDTO ticketDTO = null;
         try {
             // Chuyển đổi idCus và idFilm
             Integer customerId = Integer.parseInt(idCus);
             Integer filmId = Integer.parseInt(idFilm);
     
             // Chuyển đổi price
             Long ticketPrice = Long.parseLong(price);
     
             // Chuyển đổi idDiscount, xử lý giá trị null hoặc rỗng
             Integer discountId = (codeDiscount != null && !codeDiscount.equals("null") && !codeDiscount.isEmpty())
                     ? Integer.parseInt(codeDiscount)
                     : null;
     
             // Khởi tạo TicketDTO với các giá trị đã chuyển đổi
             ticketDTO = new TicketDTO(customerId, filmId, listSeat, discountId, ticketPrice);
         } catch (NumberFormatException e) {
             return new ResponseEntity<>("Invalid format: " + e.getMessage(), HttpStatus.BAD_REQUEST);
         }
     
         System.out.println("Ticket DTO: " + ticketDTO);
     
         // Lưu vé vào hệ thống
         iTicketServiceMinh.saveTicket(ticketDTO);
         return new ResponseEntity<>(HttpStatus.CREATED);
     }
     




    /**
     * Takes a TicketDTO object and returns the url to the sandbox payment page
     * @author MinhNV
     * @param ticketDTO
     * @return url
     * @throws UnsupportedEncodingException
     * @since 27/05/2023
     */

    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestBody TicketDTO ticketDTO) throws UnsupportedEncodingException {
        String url = iTicketServiceMinh.pay(ticketDTO);
        return new ResponseEntity<>(url, HttpStatus.OK);
    }

    /**
     * Takes a list of seat codes and movie codes then returns a SeatPrice object containing the price and seat information
     * @author MinhNV
     * @param listSeat
     * @param idFilm
     * @return SeatPriceDTO
     * @since 27/05/2023
     */

    @GetMapping("/find-by-id")
    public ResponseEntity<SeatPriceDTO> getSeatById(@RequestParam(name = "list") String listSeat,
                                            @RequestParam(name = "idFilm") Integer idFilm) {
        Film film=iMovieRepository.findFilmById(idFilm);
        double price=0;
        String[] list=listSeat.split(",");
        List<String> list1=new ArrayList<>();
        for (int i=0; i<list.length; i++){
            Seat seat = iSeatRepository.getByIdSeat(Integer.parseInt(list[i]));
            list1.add(seat.getNameSeat());
            if (seat.getTypeSeat().getIdTypeSeat()==1){
                price+=film.getNormalSeatPrice();
            }else {
                price+=film.getVipSeatPrice();
            }
        }
        return new ResponseEntity<>(new SeatPriceDTO(list1,price),HttpStatus.OK);
    }
}