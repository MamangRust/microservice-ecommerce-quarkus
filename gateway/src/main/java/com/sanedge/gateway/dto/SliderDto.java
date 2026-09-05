package com.sanedge.gateway.dto;

import java.util.List;

public class SliderDto {
    public record SliderResponse(
            int id,
            String name,
            String image,
            String createdAt,
            String updatedAt) {
        public static SliderResponse from(pb.slider.SliderCommon.SliderResponse proto) {
            return new SliderResponse(
                    proto.getId(),
                    proto.getName(),
                    proto.getImage(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static SliderResponse from(pb.slider.SliderCommon.SliderResponseDeleteAt proto) {
            return new SliderResponse(
                    proto.getId(),
                    proto.getName(),
                    proto.getImage(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllSliderResponse(
            List<SliderResponse> data,
            String status,
            String message) {
        public static FindAllSliderResponse from(pb.slider.SliderCommon.ApiResponsePaginationSlider proto) {
            return new FindAllSliderResponse(
                    proto.getDataList().stream().map(SliderResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllSliderResponse from(pb.slider.SliderCommon.ApiResponsePaginationSliderDeleteAt proto) {
            return new FindAllSliderResponse(
                    proto.getDataList().stream().map(SliderResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdSliderResponse(
            SliderResponse data,
            String status,
            String message) {
        public static FindByIdSliderResponse from(pb.slider.SliderCommon.ApiResponseSlider proto) {
            return new FindByIdSliderResponse(
                    proto.hasData() ? SliderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdSliderResponse from(pb.slider.SliderCommon.ApiResponseSliderDeleteAt proto) {
            return new FindByIdSliderResponse(
                    proto.hasData() ? SliderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateSliderRequest(
            String name,
            String image) {}

    public record CreateSliderResponse(
            SliderResponse data,
            String status,
            String message) {
        public static CreateSliderResponse from(pb.slider.SliderCommon.ApiResponseSlider proto) {
            return new CreateSliderResponse(
                    proto.hasData() ? SliderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateSliderRequest(
            String name,
            String image) {}

    public record UpdateSliderResponse(
            SliderResponse data,
            String status,
            String message) {
        public static UpdateSliderResponse from(pb.slider.SliderCommon.ApiResponseSlider proto) {
            return new UpdateSliderResponse(
                    proto.hasData() ? SliderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.slider.SliderCommon.ApiResponseSliderDeleteAt proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.slider.SliderCommon.ApiResponseSliderDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record ApiResponsePaginationSlider(
            List<SliderResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationSlider from(pb.slider.SliderCommon.ApiResponsePaginationSlider proto) {
            return new ApiResponsePaginationSlider(
                    proto.getDataList().stream().map(SliderResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponsePaginationSlider from(pb.slider.SliderCommon.ApiResponsePaginationSliderDeleteAt proto) {
            return new ApiResponsePaginationSlider(
                    proto.getDataList().stream().map(SliderResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseSlider(
            SliderResponse data,
            String status,
            String message) {
        public static ApiResponseSlider from(pb.slider.SliderCommon.ApiResponseSlider proto) {
            return new ApiResponseSlider(
                    proto.hasData() ? SliderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponseSlider from(pb.slider.SliderCommon.ApiResponseSliderDeleteAt proto) {
            return new ApiResponseSlider(
                    proto.hasData() ? SliderResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.slider.SliderCommon.ApiResponseSliderDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.slider.SliderCommon.ApiResponseSliderAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
