package com.airticket.controller;

import com.airticket.entity.Company;
import com.airticket.service.CompanyService;
import com.airticket.vo.PageResult;
import com.airticket.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @GetMapping("/list")
    public Result<PageResult<Company>> list(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) String keyword) {
        PageResult<Company> result = companyService.list(page, pageSize, keyword);
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<Company> detail(@PathVariable Integer id) {
        Company company = companyService.getById(id);
        return Result.success(company);
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody Company company) {
        companyService.add(company);
        return Result.success(null);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Company company) {
        companyService.update(company);
        return Result.success(null);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        companyService.delete(id);
        return Result.success(null);
    }
}