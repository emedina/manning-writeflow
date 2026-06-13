package com.manning.writeflow.application.ports.in;

import com.manning.writeflow.application.query.GetArticleByIdQuery;
import com.manning.writeflow.shared.dto.ArticleDTO;
import com.manning.writeflow.shared.error.Error;
import com.emedina.sharedkernel.application.annotation.UseCase;
import com.emedina.sharedkernel.query.core.QueryHandler;

/**
 * Use case to retrieve an article by its identifier.
 *
 * @author Enrique Medina Montenegro
 * @see UseCase
 */
@UseCase
public interface GetArticleByIdUseCase extends QueryHandler<Error, ArticleDTO, GetArticleByIdQuery> {
}
