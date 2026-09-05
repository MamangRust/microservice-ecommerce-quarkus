package com.sanedge.gateway.dto;

import java.util.List;

public class CategoryDto {
    public record CategoryResponse(
            int id,
            String name,
            String description,
            String slugCategory,
            String imageCategory,
            String createdAt,
            String updatedAt) {
        public static CategoryResponse from(pb.category.CategoryCommon.CategoryResponse proto) {
            return new CategoryResponse(
                    proto.getId(),
                    proto.getName(),
                    proto.getDescription(),
                    proto.getSlugCategory(),
                    proto.getImageCategory(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static CategoryResponse from(pb.category.CategoryCommon.CategoryResponseDeleteAt proto) {
            return new CategoryResponse(
                    proto.getId(),
                    proto.getName(),
                    proto.getDescription(),
                    proto.getSlugCategory(),
                    proto.getImageCategory(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllCategoryResponse(
            List<CategoryResponse> data,
            String status,
            String message) {
        public static FindAllCategoryResponse from(pb.category.CategoryCommon.ApiResponsePaginationCategory proto) {
            return new FindAllCategoryResponse(
                    proto.getDataList().stream().map(CategoryResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllCategoryResponse from(pb.category.CategoryCommon.ApiResponsePaginationCategoryDeleteAt proto) {
            return new FindAllCategoryResponse(
                    proto.getDataList().stream().map(CategoryResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdCategoryResponse(
            CategoryResponse data,
            String status,
            String message) {
        public static FindByIdCategoryResponse from(pb.category.CategoryCommon.ApiResponseCategory proto) {
            return new FindByIdCategoryResponse(
                    proto.hasData() ? CategoryResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdCategoryResponse from(pb.category.CategoryCommon.ApiResponseCategoryDeleteAt proto) {
            return new FindByIdCategoryResponse(
                    proto.hasData() ? CategoryResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateCategoryRequest(
            String name,
            String description,
            String slugCategory,
            String imageCategory) {}

    public record CreateCategoryResponse(
            CategoryResponse data,
            String status,
            String message) {
        public static CreateCategoryResponse from(pb.category.CategoryCommon.ApiResponseCategory proto) {
            return new CreateCategoryResponse(
                    proto.hasData() ? CategoryResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateCategoryRequest(
            String name,
            String description,
            String slugCategory,
            String imageCategory) {}

    public record UpdateCategoryResponse(
            CategoryResponse data,
            String status,
            String message) {
        public static UpdateCategoryResponse from(pb.category.CategoryCommon.ApiResponseCategory proto) {
            return new UpdateCategoryResponse(
                    proto.hasData() ? CategoryResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.category.CategoryCommon.ApiResponseCategoryDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.category.CategoryCommon.ApiResponseCategoryAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    // STATS RECORDS
    public record CategoryMonthPrice(
            String month,
            int categoryId,
            String categoryName,
            int orderCount,
            int itemsSold,
            int totalRevenue) {
        public static CategoryMonthPrice from(pb.category.CategoryCommon.CategoryMonthPriceResponse proto) {
            return new CategoryMonthPrice(
                    proto.getMonth(),
                    proto.getCategoryId(),
                    proto.getCategoryName(),
                    proto.getOrderCount(),
                    proto.getItemsSold(),
                    proto.getTotalRevenue()
            );
        }
    }

    public record CategoryMonthPriceResponse(
            List<CategoryMonthPrice> data,
            String status,
            String message) {
        public static CategoryMonthPriceResponse from(pb.category.CategoryCommon.ApiResponseCategoryMonthPrice proto) {
            return new CategoryMonthPriceResponse(
                    proto.getDataList().stream().map(CategoryMonthPrice::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CategoryYearPrice(
            String year,
            int categoryId,
            String categoryName,
            int orderCount,
            int itemsSold,
            int totalRevenue,
            int uniqueProductsSold) {
        public static CategoryYearPrice from(pb.category.CategoryCommon.CategoryYearPriceResponse proto) {
            return new CategoryYearPrice(
                    proto.getYear(),
                    proto.getCategoryId(),
                    proto.getCategoryName(),
                    proto.getOrderCount(),
                    proto.getItemsSold(),
                    proto.getTotalRevenue(),
                    proto.getUniqueProductsSold()
            );
        }
    }

    public record CategoryYearPriceResponse(
            List<CategoryYearPrice> data,
            String status,
            String message) {
        public static CategoryYearPriceResponse from(pb.category.CategoryCommon.ApiResponseCategoryYearPrice proto) {
            return new CategoryYearPriceResponse(
                    proto.getDataList().stream().map(CategoryYearPrice::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CategoriesMonthlyTotalPrice(
            String year,
            String month,
            int totalRevenue) {
        public static CategoriesMonthlyTotalPrice from(pb.category.CategoryCommon.CategoriesMonthlyTotalPriceResponse proto) {
            return new CategoriesMonthlyTotalPrice(
                    proto.getYear(),
                    proto.getMonth(),
                    proto.getTotalRevenue()
            );
        }
    }

    public record CategoriesMonthlyTotalPriceResponse(
            List<CategoriesMonthlyTotalPrice> data,
            String status,
            String message) {
        public static CategoriesMonthlyTotalPriceResponse from(pb.category.CategoryCommon.ApiResponseCategoryMonthlyTotalPrice proto) {
            return new CategoriesMonthlyTotalPriceResponse(
                    proto.getDataList().stream().map(CategoriesMonthlyTotalPrice::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CategoriesYearlyTotalPrice(
            String year,
            int totalRevenue) {
        public static CategoriesYearlyTotalPrice from(pb.category.CategoryCommon.CategoriesYearlyTotalPriceResponse proto) {
            return new CategoriesYearlyTotalPrice(
                    proto.getYear(),
                    proto.getTotalRevenue()
            );
        }
    }

    public record CategoriesYearlyTotalPriceResponse(
            List<CategoriesYearlyTotalPrice> data,
            String status,
            String message) {
        public static CategoriesYearlyTotalPriceResponse from(pb.category.CategoryCommon.ApiResponseCategoryYearlyTotalPrice proto) {
            return new CategoriesYearlyTotalPriceResponse(
                    proto.getDataList().stream().map(CategoriesYearlyTotalPrice::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponsePaginationCategory(
            List<CategoryResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationCategory from(pb.category.CategoryCommon.ApiResponsePaginationCategory proto) {
            return new ApiResponsePaginationCategory(
                    proto.getDataList().stream().map(CategoryResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponsePaginationCategory from(pb.category.CategoryCommon.ApiResponsePaginationCategoryDeleteAt proto) {
            return new ApiResponsePaginationCategory(
                    proto.getDataList().stream().map(CategoryResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseCategory(
            CategoryResponse data,
            String status,
            String message) {
        public static ApiResponseCategory from(pb.category.CategoryCommon.ApiResponseCategory proto) {
            return new ApiResponseCategory(
                    proto.hasData() ? CategoryResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponseCategory from(pb.category.CategoryCommon.ApiResponseCategoryDeleteAt proto) {
            return new ApiResponseCategory(
                    proto.hasData() ? CategoryResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseMonthlyTotalPrice(
            List<CategoriesMonthlyTotalPrice> data,
            String status,
            String message) {
        public static ApiResponseMonthlyTotalPrice from(pb.category.CategoryCommon.ApiResponseCategoryMonthlyTotalPrice proto) {
            return new ApiResponseMonthlyTotalPrice(
                    proto.getDataList().stream().map(CategoriesMonthlyTotalPrice::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseYearlyTotalPrice(
            List<CategoriesYearlyTotalPrice> data,
            String status,
            String message) {
        public static ApiResponseYearlyTotalPrice from(pb.category.CategoryCommon.ApiResponseCategoryYearlyTotalPrice proto) {
            return new ApiResponseYearlyTotalPrice(
                    proto.getDataList().stream().map(CategoriesYearlyTotalPrice::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseMonthlyPrice(
            List<CategoryMonthPrice> data,
            String status,
            String message) {
        public static ApiResponseMonthlyPrice from(pb.category.CategoryCommon.ApiResponseCategoryMonthPrice proto) {
            return new ApiResponseMonthlyPrice(
                    proto.getDataList().stream().map(CategoryMonthPrice::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseYearlyPrice(
            List<CategoryYearPrice> data,
            String status,
            String message) {
        public static ApiResponseYearlyPrice from(pb.category.CategoryCommon.ApiResponseCategoryYearPrice proto) {
            return new ApiResponseYearlyPrice(
                    proto.getDataList().stream().map(CategoryYearPrice::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.category.CategoryCommon.ApiResponseCategoryDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.category.CategoryCommon.ApiResponseCategoryAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
