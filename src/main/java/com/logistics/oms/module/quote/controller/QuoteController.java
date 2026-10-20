package com.logistics.oms.module.quote.controller;

import com.logistics.oms.common.Result;
import com.logistics.oms.module.quote.dto.QuoteItemVO;
import com.logistics.oms.module.quote.dto.QuoteReq;
import com.logistics.oms.module.quote.service.QuoteService;
import com.logistics.oms.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "运费试算")
@RestController
@RequestMapping("/api/quote")
@RequireRole
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Operation(summary = "运费试算（多渠道比价）")
    @PostMapping("/preview")
    public Result<List<QuoteItemVO>> preview(@RequestBody @Valid QuoteReq req) {
        return Result.ok(quoteService.preview(req));
    }
}
