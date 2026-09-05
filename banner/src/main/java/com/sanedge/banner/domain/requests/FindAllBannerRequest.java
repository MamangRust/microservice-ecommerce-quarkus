package com.sanedge.banner.domain.requests;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindAllBannerRequest {

    @QueryParam("search")
    @DefaultValue("")
    @Builder.Default
    private String search = "";

    @QueryParam("page")
    @DefaultValue("1")
    @Builder.Default
    private Integer page = 1;

    @QueryParam("pageSize")
    @DefaultValue("10")
    @Builder.Default
    private Integer pageSize = 10;
}
