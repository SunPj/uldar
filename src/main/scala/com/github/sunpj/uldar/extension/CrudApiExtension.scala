package com.github.sunpj.uldar.extension

import play.api.libs.json.{Format, Reads}

import scala.concurrent.{ExecutionContext, Future}

/**
  * SimpleCrudApiExtension
  *
  * @param name        extension name
  * @param crudService CRUD service
  * @param cmr         create model json reader
  * @param umr         update model json reader
  * @param fmr         filter model json reader
  * @param ir          entity identity json format to read/write from/to json
  * @param ec          scala execution context
  * @tparam C type of crud model types
  */
class CrudApiExtension[C <: CrudOperation](val name: String, crudService: CrudService[C])(implicit cmr: Reads[C#CreateModel],
                                   umr: Reads[C#UpdateModel],
                                   fmr: Reads[C#FilterModel],
                                   ir: Format[C#EntityId],
                                   ec: ExecutionContext) extends ApiExtension[C#User] {
  /**
    * API call handler
    *
    * Partial function from Path to ApiCall
    */
  override def apiCallHandler: PartialFunction[Seq[String], ApiCall] = {
    case Seq("create") => create
    case Seq("update") => update
    case Seq("delete") => delete
    case Seq("getEditModel") => getEditModel
    case Seq("getPreviewModel") => getPreviewModel
    case Seq("getReadModel") => getReadModel
    case Seq("fetchPreviewModels") => fetchPreviewModels
  }

  /**
    * Creates entity
    *
    * @return entity identity in json object
    */
  private def create: ApiCall = ApiCall[C#CreateModel] { request =>
    crudService.create(request.data, request.identity).map {
      case Right(entityId) => Ok("id" -> entityId)
      case Left(nonOk) => nonOk
    }
  }

  /**
    * Updates entity
    *
    * @return entity identity in json object
    */
  private def update: ApiCall = ApiCall[C#UpdateModel] { request =>
    crudService.update(request.data, request.identity).map {
      case Right(entityId) => Ok("id" -> entityId)
      case Left(nonOk) => nonOk
    }
  }

  /**
    * Deletes entity
    *
    * @return Ok if entity was deleted otherwise NotFound
    */
  private def delete: ApiCall = ApiCall[C#EntityId] { request =>
    crudService.delete(request.data, request.identity).map {
      case Right(removed) => Ok("removed" -> removed)
      case Left(nonOk) => nonOk
    }
  }

  /**
    * @return entity edit model
    */
  private def getEditModel: ApiCall = ApiCall[C#EntityId] { request =>
    crudService.getEditModel(request.data, request.identity)
  }

  /**
    * @return entity preview model
    */
  private def getPreviewModel: ApiCall = ApiCall[C#EntityId] { request =>
    crudService.getPreviewModel(request.data, request.identity)
  }

  /**
    * @return entity read model
    */
  private def getReadModel: ApiCall = ApiCall[C#EntityId] { request =>
    crudService.getReadModel(request.data, request.identity)
  }

  /**
    * @return list of preview models using filter
    */
  private def fetchPreviewModels: ApiCall = ApiCall[C#FilterModel] { request =>
    crudService.fetchPreviewModels(request.data, request.identity)
  }
}

trait CrudOperation {
  type EntityId
  type CreateModel
  type UpdateModel
  type FilterModel
  type User
}

/**
  * CRUD service
  *
  * @tparam C type of crud operation types
  */
trait CrudService[C <: CrudOperation] {
  /**
    * Creates entity using given model
    *
    * @param model entity create model
    * @param user a user identity the operation is invoked behalf of
    * @return entity identity
    */
  def create(model: C#CreateModel, user: Option[C#User]): Future[Either[NonOk, C#EntityId]]

  /**
    * Updates existing entity using given model
    *
    * @param model entity update model
    * @param user a user identity the operation is invoked behalf of
    * @return entity identity
    */
  def update(model: C#UpdateModel, user: Option[C#User]): Future[Either[NonOk, C#EntityId]]

  /**
    * Deletes entity by given identity
    *
    * @param entityId entity identity
    * @param user a user identity the operation is invoked behalf of
    * @return false if entity wasn't found otherwise true
    */
  def delete(entityId: C#EntityId, user: Option[C#User]): Future[Either[NonOk, Boolean]]

  /**
    * Returns entity edit model by given entity identity
    *
    * @param entityId entity identity
    * @param user a user identity the operation is invoked behalf of
    * @return entity edit model by given entity identity
    */
  def getEditModel(entityId: C#EntityId, user: Option[C#User]): Future[ApiCallResponse]

  /**
    * Returns entity preview model by given entity identity
    *
    * @param entityId entity identity
    * @param user a user identity the operation is invoked behalf of
    * @return entity preview model
    */
  def getPreviewModel(entityId: C#EntityId, user: Option[C#User]): Future[ApiCallResponse]

  /**
    * Returns read model by given entity identity
    *
    * @param entityId entity identity
    * @param user a user identity the operation is invoked behalf of
    * @return entity read model
    */
  def getReadModel(entityId: C#EntityId, user: Option[C#User]): Future[ApiCallResponse]

  /**
    * Returns a list of preview models by given filter
    *
    * @param filter model representing filtration conditions
    * @param user a user identity the operation is invoked behalf of
    * @return list of entity preview models
    */
  def fetchPreviewModels(filter: C#FilterModel, user: Option[C#User]): Future[ApiCallResponse]
}

/**
  * Helper decorator to pre validate models before invoking CrudService methods
  *
  * @tparam C type of crud operation types
  */
trait PreValidated[C <: CrudOperation] extends CrudService[C] {
  protected implicit val ec: ExecutionContext

  /**
    * Validates create model
    *
    * @param model entity create model
    * @param user  a user identity the operation is invoked behalf of
    * @return a set of errors or empty set
    */
  protected def validateCreateModel(model: C#CreateModel, user: Option[C#User]): Future[Set[String]]

  /**
    * Delegates call to `super` if create model is valid otherwise returns the response with errors
    *
    * @param model entity create model
    * @param user  a user identity the operation is invoked behalf of
    * @return entity identity
    */
  abstract override def create(model: C#CreateModel, user: Option[C#User]): Future[Either[NonOk, C#EntityId]] = {
    validateCreateModel(model, user).flatMap { errors =>
      if (errors.isEmpty) {
        super.create(model, user)
      } else {
        Future.successful(Left(InvalidRequest(errors)))
      }
    }
  }

  /**
    * Validates updated model
    *
    * @param model entity create model
    * @param user  a user identity the operation is invoked behalf of
    * @return a set of errors or empty set
    */
  protected def validateUpdatedModel(model: C#UpdateModel, user: Option[C#User]): Future[Set[String]]

  /**
    * Delegates call to `super` if updated model is valid otherwise returns the response with errors
    *
    * @param model entity update model
    * @param user  a user identity the operation is invoked behalf of
    * @return entity identity
    */
  abstract override def update(model: C#UpdateModel, user: Option[C#User]): Future[Either[NonOk, C#EntityId]] = {
    validateUpdatedModel(model, user).flatMap { errors =>
      if (errors.isEmpty) {
        super.update(model, user)
      } else {
        Future.successful(Left(InvalidRequest(errors)))
      }
    }
  }

  /**
    * Validates updated model
    *
    * @param entityId entity id which needs to be deleted
    * @param user     a user identity the operation is invoked behalf of
    * @return a set of errors or empty set
    */
  protected def canBeDeleted(entityId: C#EntityId, user: Option[C#User]): Future[Set[String]]

  /**
    * Delegates call to `super` if entity can be deleted otherwise returns the response with errors
    *
    * @param entityId entity identity
    * @param user     a user identity the operation is invoked behalf of
    * @return false if entity wasn't found otherwise true
    */
  abstract override def delete(entityId: C#EntityId, user: Option[C#User]): Future[Either[NonOk, Boolean]] = {
    canBeDeleted(entityId, user).flatMap { deleteErrors =>
      if (deleteErrors.isEmpty) {
        super.delete(entityId, user)
      } else {
        Future.successful(Left(InvalidRequest(deleteErrors)))
      }
    }
  }
}

/**
  * Helper decorator which checks security constraints before invoking CrudService methods
  *
  * @tparam C type of crud operation types
  */
trait Secured[C <: CrudOperation] extends CrudService[C] {
  protected implicit val ec: ExecutionContext

  /**
    * Checks whether user may create an entity using this model
    *
    * @param model entity create model
    * @param user a user identity to check the permission
    * @return whether user may create an entity using this model, where true means allowed
    */
  protected def allowedToCreate(model: C#CreateModel, user: Option[C#User]): Future[Boolean]

  /**
    * Delegates call to `super` if user is allowed to create model otherwise returns Forbidden
    *
    * @param model entity create model
    * @param user a user identity the operation is invoked behalf of
    * @return entity identity
    */
  abstract override def create(model: C#CreateModel, user: Option[C#User]): Future[Either[NonOk, C#EntityId]] = {
    allowedToCreate(model, user).flatMap { allowed =>
      if (allowed) {
        super.create(model, user)
      } else {
        Future.successful(Left(Forbidden))
      }
    }
  }

  /**
    * Checks whether user may update the entity using this given update model
    *
    * @param model entity update model
    * @param user a user identity to check the permission
    * @return whether user may update the entity using this model, where true means allowed
    */
  protected def allowedToUpdate(model: C#UpdateModel, user: Option[C#User]): Future[Boolean]

  /**
    * Delegates call to `super` if user is allowed to update the model otherwise returns Forbidden
    *
    * @param model entity update model
    * @param user a user identity the operation is invoked behalf of
    * @return entity identity
    */
  abstract override def update(model: C#UpdateModel, user: Option[C#User]): Future[Either[NonOk, C#EntityId]] = {
    allowedToUpdate(model, user).flatMap { allowed =>
      if (allowed) {
        super.update(model, user)
      } else {
        Future.successful(Left(Forbidden))
      }
    }
  }

  /**
    * Checks whether user may delete the entity by given entity id
    *
    * @param entityId entity identity
    * @param user a user identity to check the permission
    * @return whether user may delete the entity by given id, where true means allowed
    */
  protected def allowedToDelete(entityId: C#EntityId, user: Option[C#User]): Future[Boolean]

  /**
    * Delegates call to `super` if user is allowed to delete the model otherwise returns Forbidden
    *
    * @param entityId entity identity
    * @param user a user identity the operation is invoked behalf of
    * @return false if entity wasn't found otherwise true
    */
  abstract override def delete(entityId: C#EntityId, user: Option[C#User]): Future[Either[NonOk, Boolean]] = {
    allowedToDelete(entityId, user).flatMap { allowed =>
      if (allowed) {
        super.delete(entityId, user)
      } else {
        Future.successful(Left(Forbidden))
      }
    }
  }

  /**
    * Checks whether user may read edit model of entity by given id
    *
    * @param entityId entity identity
    * @param user a user identity to check the permission
    * @return whether user may read edit model of entity by given id, where true means allowed
    */
  protected def allowedToEdit(entityId: C#EntityId, user: Option[C#User]): Future[Boolean]

  /**
    * Delegates call to `super` if user is allowed to read the edit model otherwise returns Forbidden
    *
    * @param entityId entity identity
    * @param user a user identity the operation is invoked behalf of
    * @return entity edit model by given entity identity
    */
  abstract override def getEditModel(entityId: C#EntityId, user: Option[C#User]): Future[ApiCallResponse] = {
    allowedToEdit(entityId, user).flatMap { allowed =>
      if (allowed) {
        super.getEditModel(entityId, user)
      } else {
        Future.successful(Forbidden)
      }
    }
  }

  /**
    * Checks whether user is allowed to read preview model of entity by given id
    *
    * @param entityId entity identity
    * @param user a user identity to check the permission
    * @return whether user is allowed to read preview model of entity by given id, where true means allowed
    */
  protected def allowedGetPreviewModel(entityId: C#EntityId, user: Option[C#User]): Future[Boolean]

  /**
    * Delegates call to `super` if user is allowed to read the preview model otherwise returns Forbidden
    *
    * @param entityId entity identity
    * @param user a user identity the operation is invoked behalf of
    * @return entity preview model
    */
  abstract override def getPreviewModel(entityId: C#EntityId, user: Option[C#User]): Future[ApiCallResponse] = {
    allowedGetPreviewModel(entityId, user).flatMap { allowed =>
      if (allowed) {
        super.getPreviewModel(entityId, user)
      } else {
        Future.successful(Forbidden)
      }
    }
  }

  /**
    * Checks whether user is allowed to get read model of entity by given id
    *
    * @param entityId entity identity
    * @param user a user identity to check the permission
    * @return whether user is allowed to get read model of entity by given id, where true means allowed
    */
  protected def allowedGetReadModel(entityId: C#EntityId, user: Option[C#User]): Future[Boolean]

  /**
    * Delegates call to `super` if user is allowed to read the model otherwise returns Forbidden
    *
    * @param entityId entity identity
    * @param user a user identity the operation is invoked behalf of
    * @return entity read model
    */
  abstract override def getReadModel(entityId: C#EntityId, user: Option[C#User]): Future[ApiCallResponse] = {
    allowedGetReadModel(entityId, user).flatMap { allowed =>
      if (allowed) {
        super.getReadModel(entityId, user)
      } else {
        Future.successful(Forbidden)
      }
    }
  }

  /**
    * Checks whether user is allowed to fetch preview models by given filter
    *
    * @param filter model representing filtration conditions
    * @param user a user identity to check the permission
    * @return whether user is allowed to fetch preview models by given filter, where true means allowed
    */
  protected def allowedFetchPreviewModels(filter: C#FilterModel, user: Option[C#User]): Future[Boolean]

  /**
    * Delegates call to `super` if user is allowed to fetch models otherwise returns Forbidden
    *
    * @param filter model representing filtration conditions
    * @param user a user identity the operation is invoked behalf of
    * @return list of entity preview models
    */
  abstract override def fetchPreviewModels(filter: C#FilterModel, user: Option[C#User]): Future[ApiCallResponse] = {
    allowedFetchPreviewModels(filter, user).flatMap { allowed =>
      if (allowed) {
        super.fetchPreviewModels(filter, user)
      } else {
        Future.successful(Forbidden)
      }
    }
  }
}

trait CrudRepository[T, ID] {
  def create(entity: T): Future[ID]
  def delete(id: ID): Future[Boolean]
  def update(id: ID, entity: T): Future[Boolean]
  def fetch(id: ID): Future[Option[T]]
}
