package com.airticket.controller;

import com.airticket.entity.News;
import com.airticket.service.NewsService;
import com.airticket.vo.PageResult;
import com.airticket.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @GetMapping("/list")
    public Result<PageResult<News>> list(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<News> result = newsService.list(page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<News> detail(@PathVariable Integer id) {
        News news = newsService.getById(id);
        return Result.success(news);
    }
}