package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.*
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.utils.ActionUtils
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.environment.api.RemoteEnvResource
import com.tencent.devops.environment.api.RemoteNodeResource
import com.tencent.devops.process.api.service.ServiceAuthPipelineResource
import com.tencent.devops.project.api.service.ServiceAuthResource
import com.tencent.devops.repository.api.ServiceAuthRepositoryResource
import com.tencent.devops.ticket.api.ServiceAuthCallbackResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ResourceService @Autowired constructor(
    val client: Client,
    val remoteAuthService: RemoteAuthService
) {

    fun getProjectInfo(callBackInfo: CallbackRequestDTO, method: CallbackMethodEnum, token: String): CallbackBaseResponseDTO {
        if(method == CallbackMethodEnum.LIST_INSTANCE) {
            return getProjectList(callBackInfo.page, method, token)
        } else if(method == CallbackMethodEnum.FETCH_INSTANCE_INFO) {
            val ids = callBackInfo.filter.idList.map { it.toString() }
            return getProjectInfo(ids, callBackInfo.filter.attributeList)
        }
        return getProjectList(callBackInfo.page, method, token)
    }

    fun getResourceList(
        projectId: String,
        actionType: String,
        method: CallbackMethodEnum,
        page: PageInfoDTO?,
        token: String
    ): ListInstanceResponseDTO? {
        logger.info("getResourceList project[$projectId] method[$method], page[$page],token[$token],actionType[$actionType]")
        checkToken(token)
        var offset = 0
        var limit = 10
        if(page != null) {
            offset = page.offset.toInt()
            limit = page.limit.toInt()
        }
        val resourceType = if (actionType.contains("env_node")) {
            AuthResourceType.ENVIRONMENT_ENV_NODE.value
        } else {
            ActionUtils.actionType(actionType)
        }
        if (AuthResourceType.get(resourceType) == null) {
            logger.warn("getResourceList actionType is not exits,actionType $actionType, resourceType $resourceType")
            throw RuntimeException("资源类型不存在")
        }
        var result: ListInstanceResponseDTO? = null
        when (resourceType) {
            AuthResourceType.PIPELINE_DEFAULT.value -> result = getPipeline(projectId, offset, limit)
            AuthResourceType.CODE_REPERTORY.value -> result = getRepository(projectId, offset, limit)
            AuthResourceType.ENVIRONMENT_ENVIRONMENT.value -> result = getEnv(projectId, offset, limit)
            AuthResourceType.ENVIRONMENT_ENV_NODE.value -> result = getNode(projectId, offset, limit)
            AuthResourceType.TICKET_CREDENTIAL.value -> result = getCredential(projectId, offset, limit)
            AuthResourceType.TICKET_CERT.value -> result = getCert(projectId, offset, limit)
            else -> null
        }
        return result
    }

    private fun getPipeline(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val pipelineInfos =
            client.get(ServiceAuthPipelineResource::class)
                .pipelineList(projectId, offset, limit).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (pipelineInfos?.records == null) {
            logger.info("$projectId 项目下无流水线")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.pipelineId
            entity.displayName = it.pipelineName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos?.count}")
        data.count = pipelineInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun getRepository(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val repositoryInfos =
            client.get(ServiceAuthRepositoryResource::class)
                .listByProjects(setOf(projectId), offset, limit).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (repositoryInfos?.records == null) {
            logger.info("$projectId 项目下无代码库")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        repositoryInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.repositoryHashId
            entity.displayName = it.aliasName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${repositoryInfos?.count}")
        data.count = repositoryInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun getCredential(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val credentialInfos =
            client.get(ServiceAuthCallbackResource::class)
                .listCredential(projectId, offset, limit).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (credentialInfos?.records == null) {
            logger.info("$projectId 项目下无凭证")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        credentialInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.credentialId
            entity.displayName = it.credentialRemark
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${credentialInfos?.count}")
        data.count = credentialInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun getCert(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val certInfos =
                client.get(ServiceAuthCallbackResource::class)
                        .listCert(projectId, offset, limit).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (certInfos?.records == null) {
            logger.info("$projectId 项目下无凭证")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        certInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.credentialId
            entity.displayName = it.certRemark
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${certInfos?.count}")
        data.count = certInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun getNode(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val nodeInfos =
            client.get(RemoteNodeResource::class)
                .listNodeByPage(projectId, offset, limit).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (nodeInfos?.records == null) {
            logger.info("$projectId 项目下无节点")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        nodeInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.nodeHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${nodeInfos?.count}")
        data.count = nodeInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun getEnv(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val envInfos =
            client.get(RemoteEnvResource::class)
                .listEnvByPage(projectId, offset, limit).data
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        if (envInfos?.records == null) {
            logger.info("$projectId 项目下无环境")
            result.code = 0
            result.message = "无数据"
            result.data = data
            return result
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        envInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.envHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${envInfos?.count}")
        data.count = envInfos?.count
        data.result = entityInfo
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    private fun checkToken(token: String) {
        if (!remoteAuthService.checkToken(token)) {
            logger.warn("auth callBack checkToken is fail $token")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.TOKEN_TICKET_FAIL))
        }
    }

    private fun getProjectList(page: PageInfoDTO?, method: CallbackMethodEnum, token: String): ListInstanceResponseDTO {
        logger.info("getProjectList method $method, page $page token $token")
        checkToken(token)
        var offset = 0
        var limit = 10
        if(page != null) {
            offset = page.offset.toInt()
            limit = page.limit.toInt()
        }
        val projectRecords =
                client.get(ServiceAuthResource::class).list(offset, limit).data
        logger.info("projectRecords $projectRecords")
        val count = projectRecords?.count ?: 0L
        val projectInfo = mutableListOf<InstanceInfoDTO>()
        projectRecords?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.englishName
            projectInfo.add(entity)
        }
        logger.info("projectInfo $projectInfo")
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        data.count = count
        data.result = projectInfo
        result.code = 0L
        result.message = ""
        result.data = data
        logger.info("result $result")
        return result
    }

    private fun getProjectInfo(idList: List<String>, attrs: List<String>): FetchInstanceInfoResponseDTO {
        logger.info("getProjectInfo ids[$idList] attrs[$attrs]")
        val ids = idList.toSet()
        val projectInfo = client.get(ServiceAuthResource::class).getByIds(ids).data
        logger.info("projectRecords $projectInfo")
        val entityList = mutableListOf<InstanceInfoDTO>()
        projectInfo?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.englishName
            entityList.add(entity)
        }
        val result = FetchInstanceInfoResponseDTO()
        result.code = 0
        result.message = ""
        result.data = entityList.toList()
        return result
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}