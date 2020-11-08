package admin_panel.entityes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Lesson {
    String groupName = "-";
    String number = "-";
    String discipline = "-";
    String audience = "-";
    String teacher = "-";
}
