package com.sanedge.gateway.dto;

public record PaginationMeta(
        int currentPage,
        int pageSize,
        int totalPages,
        int totalRecords) {

    public static PaginationMeta from(pb.Api.PaginationMeta proto) {
        return new PaginationMeta(
                proto.getCurrentPage(),
                proto.getPageSize(),
                proto.getTotalPages(),
                proto.getTotalRecords()
        );
    }
}
