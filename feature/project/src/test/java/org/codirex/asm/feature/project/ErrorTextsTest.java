package org.codirex.asm.feature.project;

import static org.junit.Assert.assertNotEquals;

import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectType;
import org.junit.Test;

/** Guards against adding a domain error code and forgetting its text. */
public final class ErrorTextsTest {

  @Test
  public void everyNameErrorHasItsOwnText() {
    String[] codes = {
      ProjectName.ERROR_EMPTY,
      ProjectName.ERROR_TOO_LONG,
      ProjectName.ERROR_RESERVED,
      ProjectName.ERROR_CHARACTER
    };
    for (String code : codes) {
      assertNotEquals(code, R.string.error_invalid, ErrorTexts.nameError(code));
    }
  }

  @Test
  public void everyPackageErrorHasItsOwnText() {
    String[] codes = {
      PackageName.ERROR_EMPTY,
      PackageName.ERROR_TOO_FEW_SEGMENTS,
      PackageName.ERROR_SEGMENT_START,
      PackageName.ERROR_CHARACTER,
      PackageName.ERROR_KEYWORD
    };
    for (String code : codes) {
      assertNotEquals(code, R.string.error_invalid, ErrorTexts.packageError(code));
    }
  }

  @Test
  public void everyReasonAndTypeHasAText() {
    for (Reason reason : Reason.values()) {
      ErrorTexts.reason(reason);
    }
    for (ProjectType type : ProjectType.values()) {
      ProjectTypes.label(type);
    }
  }
}
