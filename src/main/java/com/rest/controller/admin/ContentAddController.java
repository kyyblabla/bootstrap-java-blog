package com.rest.controller.admin;

import com.rest.Request.AddContentRequest;
import com.rest.annotation.NeedAuth;
import com.rest.bean.User;
import com.rest.converter.ContentConverter;
import com.rest.domain.Content;
import com.rest.domain.ContentTime;
import com.rest.mapper.ContentMapper;
import com.rest.mapper.ContentTimeMapper;
import com.rest.service.SearchService;
import com.rest.utils.AntiSamyUtils;
import com.rest.utils.MarkDownUtil;
import com.rest.utils.SessionUtils;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Calendar;

/**
 * Created by bruce.ge on 2016/11/6.
 */
@Controller
public class ContentAddController {
    @Autowired
    private ContentMapper contentMapper;

    @Autowired
    private ContentTimeMapper contentTimeMapper;

    @Autowired
    private SearchService searchService;

    @PostMapping("/addContent")
    @ResponseBody
    @NeedAuth
    public boolean addContent(AddContentRequest request) throws ScanException, PolicyException {
        //which shall redirect when ok.
        request.setTitle(AntiSamyUtils.getCleanHtml(request.getTitle()));
        Calendar calendar = Calendar.getInstance();
        User currentUser =
                SessionUtils.getCurrentUser();
        if (currentUser == null) {
            //todo never shall happen.
            return false;
        }
        Content content = ContentConverter.convertToContent(request,currentUser);
        contentMapper.addContent(content);
        ContentTime time = new ContentTime();
        time.setYear(calendar.get(Calendar.YEAR));
        time.setMonth(calendar.get(Calendar.MONTH) + 1);
        time.setDay(calendar.get(Calendar.DAY_OF_MONTH));
        time.setContent_id(content.getId());
        contentTimeMapper.insert(time);
        //add data to lucene.
        new Thread(new Runnable() {
            @Override
            public void run() {
                searchService.addSource(request.getTitle(), MarkDownUtil.removeMark(request.getSourceContent()), content.getId());
            }
        }).start();
        return true;
    }

    @NeedAuth(redirectBack = true)
    @GetMapping("/add")
    public String addPage() {
        return "add";
    }
}