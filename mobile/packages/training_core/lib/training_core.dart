/// Training logic shared by the phone and watch apps.
///
/// Everything here is pure Dart: no Flutter, no storage, no network. That keeps
/// it fast to test and safe to run on any isolate or device.
library;

export 'src/load/effective_load.dart';
export 'src/load/load_catalogue.dart';
export 'src/load/load_modifier.dart';
export 'src/load/load_profile.dart';
export 'src/pain/pain_guidance.dart';
export 'src/skills/skill_catalogue.dart';
export 'src/skills/skill_path.dart';
export 'src/strength/one_rep_max.dart';
