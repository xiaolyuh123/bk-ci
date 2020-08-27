package com.tencent.devops.project.service.impl

import com.tencent.devops.common.auth.api.*
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.service.ProjectPermissionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.lang.RuntimeException

class V3ProjectPermissionServiceImpl @Autowired constructor(
    val client: Client,
    private val authProjectApi: AuthProjectApi,
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val projectDao: ProjectDao,
    private val dslContext: DSLContext
) : ProjectPermissionService {

    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String): Boolean {
        return authProjectApi.isProjectUser(
            user = userId,
            serviceCode = projectAuthServiceCode,
            projectCode = projectCode,
            group = null
        )
    }

    // 创建项目
    override fun createResources(
        userId: String,
        accessToken: String?,
        resourceRegisterInfo: ResourceRegisterInfo
    ): String {
        val validateCreatePermission = authPermissionApi.validateUserResourcePermission(userId, projectAuthServiceCode, AuthResourceType.PROJECT, "", AuthPermission.CREATE)
        if(!validateCreatePermission) {
            throw RuntimeException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.COLLECTION_CANCEL_SUCC))
        }
        authResourceApi.createResource(userId, projectAuthServiceCode, AuthResourceType.PROJECT, resourceRegisterInfo.resourceCode, resourceRegisterInfo.resourceCode, resourceRegisterInfo.resourceName)
        return ""
    }

    override fun deleteResource(projectCode: String) {
        return
    }

    override fun modifyResource(projectCode: String, projectName: String) {
        val validatePermission = authPermissionApi.validateUserResourcePermission(userId, projectAuthServiceCode, AuthResourceType.PROJECT, projectCode, AuthPermission.CREATE)
        return
    }

    override fun getUserProjects(userId: String): List<String> {
        val projects = authProjectApi.getUserProjects(
            serviceCode = projectAuthServiceCode,
            userId = userId,
            supplier = null
        )

        if (projects == null || projects.isEmpty()) {
            return emptyList()
        }

        val projectList = mutableListOf<String>()
        return if (projects[0] == "*") {
            projectDao.getAllProject(dslContext).filter { projectList.add(it.englishName) }
            projectList
        } else {
            projects.map {
                projectList.add(it.trim())
            }
            projectList
        }
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        return authProjectApi.getUserProjectsAvailable(
            userId = userId,
            serviceCode = projectAuthServiceCode,
            supplier = null
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}