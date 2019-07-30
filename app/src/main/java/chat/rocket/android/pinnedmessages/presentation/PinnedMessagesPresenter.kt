package chat.rocket.android.pinnedmessages.presentation

import chat.rocket.android.chatroom.uimodel.UiModelMapper
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.db.DatabaseManager
import chat.rocket.android.server.infrastructure.RocketChatClientFactory
import chat.rocket.android.util.extension.launchUI
import chat.rocket.common.RocketChatException
import chat.rocket.common.model.roomTypeOf
import chat.rocket.common.util.ifNull
import chat.rocket.core.RocketChatClient
import chat.rocket.core.internal.rest.getPinnedMessages
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class PinnedMessagesPresenter @Inject constructor(
    private val view: PinnedMessagesView,
    private val strategy: CancelStrategy,
    private val dbManager: DatabaseManager,
    @Named("currentServer") private val currentServer: String?,
    private val mapper: UiModelMapper,
    val factory: RocketChatClientFactory
) {
    private val client: RocketChatClient? = currentServer?.let { factory.get(it) }
    private var offset: Int = 0

    /**
     * Loads all pinned messages for the given room id.
     *
     * @param roomId The id of the room to get pinned messages from.
     */
    fun loadPinnedMessages(roomId: String) {
        launchUI(strategy) {
            try {
                view.showLoading()
                dbManager.getRoom(roomId)?.let {
                    client?.getPinnedMessages(roomId, roomTypeOf(it.chatRoom.type), offset)?.let {
                        val messageList = mapper.map(it.result, asNotReversed = true)
                        view.showPinnedMessages(messageList)
                        offset += 1 * 30
                    }
                }.ifNull {
                    Timber.e("Couldn't find a room with id: $roomId at current server.")
                }
            } catch (exception: RocketChatException) {
                Timber.e(exception)
            } finally {
                view.hideLoading()
            }
        }
    }
}