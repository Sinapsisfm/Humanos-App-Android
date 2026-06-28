package eco.humanos.android.core.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.domain.OutingStateMachine
import eco.humanos.android.core.outdoor.domain.OutingStatus
import org.junit.Test

class OutingStateTest {

    @Test fun `permite transiciones validas`() {
        assertThat(OutingStateMachine.canTransition(OutingStatus.DRAFT, OutingStatus.PREPARED)).isTrue()
        assertThat(OutingStateMachine.canTransition(OutingStatus.PREPARED, OutingStatus.ACTIVE)).isTrue()
        assertThat(OutingStateMachine.canTransition(OutingStatus.ACTIVE, OutingStatus.INCIDENT)).isTrue()
        assertThat(OutingStateMachine.canTransition(OutingStatus.COMPLETED, OutingStatus.ARCHIVED)).isTrue()
    }

    @Test fun `rechaza transiciones invalidas`() {
        assertThat(OutingStateMachine.canTransition(OutingStatus.DRAFT, OutingStatus.ACTIVE)).isFalse()
        assertThat(OutingStateMachine.canTransition(OutingStatus.ARCHIVED, OutingStatus.ACTIVE)).isFalse()
        assertThat(OutingStateMachine.canTransition(OutingStatus.COMPLETED, OutingStatus.ACTIVE)).isFalse()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `assertTransition lanza en transicion invalida`() {
        OutingStateMachine.assertTransition(OutingStatus.ARCHIVED, OutingStatus.ACTIVE)
    }
}
