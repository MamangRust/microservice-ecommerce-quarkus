package com.sanedge.cart.domain.requests;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@Schema(description = "Request untuk menghapus cart berdasarkan daftar ID cart")
public class DeleteCartRequest {

    @NotEmpty
    @Schema(description = "Daftar ID cart yang akan dihapus", example = "[1, 2, 3]")
    private List<Integer> cartIds;
}