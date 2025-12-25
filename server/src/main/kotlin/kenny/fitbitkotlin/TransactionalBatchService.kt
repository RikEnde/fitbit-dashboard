package kenny.fitbitkotlin

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TransactionalBatchService(
    private val entityManager: EntityManager
) {

    @Transactional(propagation = Propagation.REQUIRED)
    fun <T> saveBatchWithFlush(batch: List<T>, saveOperation: (List<T>) -> Unit) {
        saveOperation(batch)
        entityManager.flush()
        entityManager.clear()
    }
}