package com.airticket.controller;

import com.airticket.dto.LoginDTO;
import com.airticket.entity.*;
import com.airticket.service.*;
import com.airticket.vo.PageResult;
import com.airticket.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private HbxxService hbxxService;

    @Autowired
    private HbxxCabinService hbxxCabinService;

    @Autowired
    private JpddService jpddService;

    @Autowired
    private TdjpService tdjpService;

    @Autowired
    private NewsService newsService;

    @Autowired
    private MenuService menuService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        Map<String, Object> result = adminService.login(dto.getUsername(), dto.getPassword());
        return Result.success(result);
    }

    @GetMapping("/info")
    public Result<Admin> info(HttpServletRequest request) {
        Integer adminId = (Integer) request.getAttribute("adminId");
        Admin admin = adminService.getInfo(adminId);
        return Result.success(admin);
    }

    // ========== User Management ==========
    @GetMapping("/user/list")
    public Result<PageResult<com.airticket.vo.UserVO>> userList(@RequestParam(defaultValue="1") Integer page,
            @RequestParam(defaultValue="10") Integer pageSize,@RequestParam(defaultValue="") String username) {
        String pattern="%"+username+"%";
        Long total=db.queryForObject("SELECT COUNT(*) FROM `user` WHERE username LIKE ?",Long.class,pattern);
        List<com.airticket.vo.UserVO> records=db.query("SELECT id,username,name,gender,CASE WHEN LENGTH(phone)>=7 THEN CONCAT(LEFT(phone,3),'****',RIGHT(phone,4)) ELSE '***' END AS phone,email,CASE WHEN LENGTH(identity)>=7 THEN CONCAT(LEFT(identity,3),'***********',RIGHT(identity,4)) ELSE '***' END AS identity FROM `user` WHERE username LIKE ? ORDER BY id DESC LIMIT ? OFFSET ?",new org.springframework.jdbc.core.BeanPropertyRowMapper<>(com.airticket.vo.UserVO.class),pattern,pageSize,(page-1)*pageSize);
        return Result.success(new PageResult<>(total,records,page,pageSize));
    }

    // ========== Company Management ==========
    @GetMapping("/company/list")
    public Result<PageResult<Company>> companyList(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String keyword) {
        PageResult<Company> result = companyService.list(page, pageSize, keyword);
        return Result.success(result);
    }

    @PostMapping("/company/add")
    public Result<Void> companyAdd(@RequestBody Company company) {
        companyService.add(company);
        return Result.success();
    }

    @PutMapping("/company/update")
    public Result<Void> companyUpdate(@RequestBody Company company) {
        companyService.update(company);
        return Result.success();
    }

    @DeleteMapping("/company/delete/{id}")
    public Result<Void> companyDelete(@PathVariable Integer id) {
        companyService.delete(id);
        return Result.success();
    }

    // ========== Flight Management ==========
    @GetMapping("/hbxx/search")
    public Result<PageResult<Hbxx>> hbxxSearch(@RequestParam(required = false) String start,
                                               @RequestParam(required = false) String end,
                                               @RequestParam(required = false) String starttime,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes().setAttribute("isAdminQuery",true,0);
        PageResult<Hbxx> result = hbxxService.search(start, end, starttime, page, pageSize);
        return Result.success(result);
    }

    @PostMapping("/hbxx/add")
    public Result<Void> hbxxAdd(@RequestBody Hbxx hbxx) {
        hbxxService.add(hbxx);
        return Result.success();
    }

    @PutMapping("/hbxx/update")
    public Result<Void> hbxxUpdate(@RequestBody Hbxx hbxx) {
        hbxxService.update(hbxx);
        return Result.success();
    }

    @DeleteMapping("/hbxx/delete/{id}")
    public Result<Void> hbxxDelete(@PathVariable Integer id) {
        hbxxService.delete(id);
        return Result.success();
    }

    @GetMapping("/hbxx/{hbid}/cabins")
    public Result<List<HbxxCabin>> hbxxCabins(@PathVariable Integer hbid) {
        List<HbxxCabin> cabins = hbxxCabinService.getCabinsByHbid(hbid);
        return Result.success(cabins);
    }

    @PostMapping("/hbxx/cabin/add")
    public Result<Void> cabinAdd(@RequestBody HbxxCabin cabin) {
        hbxxCabinService.add(cabin);
        return Result.success();
    }

    @PutMapping("/hbxx/cabin/update")
    public Result<Void> cabinUpdate(@RequestBody HbxxCabin cabin) {
        hbxxCabinService.update(cabin);
        return Result.success();
    }

    @DeleteMapping("/hbxx/cabin/delete/{cabinId}")
    public Result<Void> cabinDelete(@PathVariable Integer cabinId) {
        hbxxCabinService.deleteById(cabinId);
        return Result.success();
    }

    // ========== Order Management ==========
    @GetMapping("/jpdd/list")
    public Result<PageResult<com.airticket.vo.JpddVO>> jpddList(@RequestParam(required = false) String flightName,
                                                                 @RequestParam(required = false) String userName,
                                                                 @RequestParam(defaultValue = "1") Integer page,
                                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<com.airticket.vo.JpddVO> result = jpddService.adminQuery(flightName, userName, page, pageSize);
        return Result.success(result);
    }

    // ========== Refund Management ==========
    @GetMapping("/tdjp/list")
    public Result<PageResult<com.airticket.vo.TdjpVO>> tdjpList(@RequestParam(required = false) Integer process,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<com.airticket.vo.TdjpVO> result = tdjpService.list(page, pageSize, process);
        return Result.success(result);
    }

    @PutMapping("/tdjp/approve/{id}")
    public Result<Void> tdjpApprove(@PathVariable Integer id) {
        tdjpService.approve(id);
        return Result.success();
    }

    // ========== News Management ==========
    @GetMapping("/news/list")
    public Result<PageResult<News>> newsList(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<News> result = newsService.list(page, pageSize);
        return Result.success(result);
    }

    @PostMapping("/news/add")
    public Result<Void> newsAdd(@RequestBody News news) {
        newsService.add(news);
        return Result.success();
    }

    @PutMapping("/news/update")
    public Result<Void> newsUpdate(@RequestBody News news) {
        newsService.update(news);
        return Result.success();
    }

    @DeleteMapping("/news/delete/{id}")
    public Result<Void> newsDelete(@PathVariable Integer id) {
        newsService.delete(id);
        return Result.success();
    }

    // ========== Menu Management (Super Admin Only) ==========
    @GetMapping("/menu/list")
    public Result<List<Menu>> menuList() {
        List<Menu> list = menuService.findAll();
        return Result.success(list);
    }

    @PostMapping("/menu/add")
    public Result<Void> menuAdd(@RequestBody Menu menu) {
        menuService.add(menu);
        return Result.success();
    }

    @PutMapping("/menu/update")
    public Result<Void> menuUpdate(@RequestBody Menu menu) {
        menuService.update(menu);
        return Result.success();
    }

    @DeleteMapping("/menu/delete/{id}")
    public Result<Void> menuDelete(@PathVariable Integer id) {
        menuService.delete(id);
        return Result.success();
    }

    // ========== Role Management (Super Admin Only) ==========
    @GetMapping("/role/list")
    public Result<List<Admin>> roleList() {
        List<Admin> list = adminService.findAll();
        return Result.success(list);
    }

    @PostMapping("/role/add")
    public Result<Void> roleAdd(@RequestBody Admin admin) {
        adminService.add(admin);
        return Result.success();
    }

    @PutMapping("/role/update")
    public Result<Void> roleUpdate(@RequestBody Admin admin) {
        adminService.update(admin);
        return Result.success();
    }

    @DeleteMapping("/role/delete/{id}")
    public Result<Void> roleDelete(@PathVariable Integer id) {
        adminService.delete(id);
        return Result.success();
    }
    @Autowired private com.airticket.service.BookingService booking;
    @PutMapping("/tdjp/reject/{id}")
    public Result<Void> reject(@PathVariable Integer id,@RequestBody Map<String,String> body){booking.review(id,false,body.get("reason"),com.airticket.util.CurrentUser.id());return Result.success();}
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    @GetMapping("/jpdd/log/{id}")
    public Result<List<Map<String,Object>>> logs(@PathVariable Integer id){List<Map<String,Object>> rows=db.queryForList("SELECT log_id,action,old_status,new_status,log_time,operator,remark FROM order_log WHERE ddid=? ORDER BY log_id",id);for(Map<String,Object> row:rows)row.put("remark",com.airticket.service.CustomerPresentation.remark(row.get("remark")));return Result.success(rows);}
    @PostMapping("/logout")
    public Result<Void> logout(){db.update("UPDATE admin SET auth_version=auth_version+1 WHERE id=?",com.airticket.util.CurrentUser.id());return Result.success();}
}
