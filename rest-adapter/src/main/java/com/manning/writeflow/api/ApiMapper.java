package com.manning.writeflow.api;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.manning.writeflow.shared.dto.ArticleDTO;

/**
 * Mappings for the API between DTOs and API responses.
 *
 * @author Enrique Medina Montenegro
 */
@Mapper
interface ApiMapper {

    ApiMapper INSTANCE = Mappers.getMapper(ApiMapper.class);

    ApiResponse.Article toArticleResponse(final ArticleDTO dto);

}
