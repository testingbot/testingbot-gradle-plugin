package com.testingbot.gradle.internal

/** Response of `POST /storage` — `{ "app_url": "tb://<appkey>" }`. */
internal data class StorageUploadResponse(val app_url: String?)

/** Response of `POST /app-automate/espresso/app` and `.../tests` — `{ "id": <projectId> }`. */
internal data class EspressoIdResponse(val id: Long?)

/** A single run entry returned by `POST /app-automate/espresso/{id}/run`. */
internal data class EspressoRunInfo(val id: Long?, val capabilities: Any?)

/** Response of `POST /app-automate/espresso/{id}/run`. */
internal data class EspressoRunResult(
    val success: Boolean?,
    val id: Long?,
    val runs: List<EspressoRunInfo>?,
)

/** A run entry inside the project status payload. `success` is `1` when the run passed. */
internal data class EspressoRunStatus(val id: Long?, val state: String?, val success: Int?)

/** Response of `GET /app-automate/espresso/{id}`. */
internal data class EspressoStatusResponse(
    val runs: List<EspressoRunStatus>?,
    val success: Boolean?,
    val completed: Boolean?,
)

/**
 * A device entry from `GET /devices`. Field names match the runtime JSON payload
 * (`Device#api_json_structure`): the OS version is `version` (not `platform_version`) and the
 * model is `model_number`.
 */
internal data class Device(
    val id: Long?,
    val name: String?,
    val model_number: String?,
    val platform_name: String?,
    val version: String?,
    val available: Boolean?,
)
