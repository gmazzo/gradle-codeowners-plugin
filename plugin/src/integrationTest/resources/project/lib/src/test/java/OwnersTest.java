import org.junit.Test;
import org.test.lib.LibClass;

import static java.util.Collections.singletonList;

import static com.github.gmazzo.codeowners.CodeOwners.getCodeOwners;

import static org.junit.Assert.assertEquals;

public class OwnersTest {

    @Test
    public void ownerOfSelf() {
        assertEquals(singletonList("kotlin-devs"), getCodeOwners(LibClass.class));
    }

}
