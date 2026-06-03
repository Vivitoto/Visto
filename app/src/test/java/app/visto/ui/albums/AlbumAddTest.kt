package app.visto.ui.albums

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumAddTest {

    @Test
    fun updatePathAutoFillsNameUntilUserEditsIt() {
        var state = AlbumAddFormState()
        state = AlbumAddFormReducer.updatePath(state, "/Photos/Family")
        assertEquals("Family", state.name)
        assertTrue(state.nameAutoFilled)

        state = AlbumAddFormReducer.updateName(state, "家庭照片")
        assertEquals("家庭照片", state.name)

        // Once the user customized the name, switching path keeps their name.
        state = AlbumAddFormReducer.updatePath(state, "/Photos/Travel")
        assertEquals("家庭照片", state.name)
    }

    @Test
    fun validatorRejectsEmptyAndNonAbsolutePaths() {
        val empty = AlbumAddValidator.validate(AlbumAddFormState(name = "x", path = "  "), emptySet())
        assertTrue(empty is AlbumAddValidator.Result.Err)

        val noSlash = AlbumAddValidator.validate(AlbumAddFormState(name = "x", path = "Photos"), emptySet())
        assertTrue(noSlash is AlbumAddValidator.Result.Err)
    }

    @Test
    fun validatorRejectsDuplicates() {
        val dup = AlbumAddValidator.validate(
            state = AlbumAddFormState(name = "Family", path = "/Photos/Family"),
            existingPaths = setOf("/Photos/Family"),
        )
        assertTrue(dup is AlbumAddValidator.Result.Err)
    }

    @Test
    fun validatorNormalizesTrailingSlashAndDerivesName() {
        val r = AlbumAddValidator.validate(
            state = AlbumAddFormState(name = "  ", path = "/Photos/Family/"),
            existingPaths = emptySet(),
        )
        assertTrue(r is AlbumAddValidator.Result.Ok)
        val ok = r as AlbumAddValidator.Result.Ok
        assertEquals("/Photos/Family", ok.path)
        assertEquals("Family", ok.name)
    }
}
