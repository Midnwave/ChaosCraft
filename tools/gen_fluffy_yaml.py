#!/usr/bin/env python3
"""
Regenerate the bundled FluffyMode fluffy.yml so every key has its full
comment block, matching what FluffyConfig.createDefaults() writes via
defaults.setComments(...) in Java.

Approach:
  * Parse FluffyConfig.java's createDefaults() body to extract
    (key.path, value) and (key.path, [comment lines]) pairs.
  * Parse MobSpawnConfig.writeDefaults() the same way for the
    "mob-spawning.*" section (FluffyConfig calls
    MobSpawnConfig.writeDefaults(defaults, new ArrayList<>())).
  * Parse buildDefaultRainMobs() to recreate the rain-from-sky.mobs
    list-of-maps default (mmMob/vanillaMob helpers).
  * Emit a YAML file in the same shape Bukkit's YamlConfiguration would
    save: 2-space indent, comments emitted as "# ..." immediately above
    each key, in insertion order.

Output: D:/CC/ChaosCraft/dist/fluffy_dragdrop/plugins/ChaosCraft/modes/fluffy/fluffy.yml
"""
from __future__ import annotations
import re
from pathlib import Path
from typing import Any

JAVA_FLUFFY = Path(r"D:\CC\ChaosCraft\src\main\java\com\blockforge\chaoscraft\modes\fluffy\FluffyConfig.java")
JAVA_MOBSPAWN = Path(r"D:\CC\ChaosCraft\src\main\java\com\blockforge\chaoscraft\services\mobspawn\MobSpawnConfig.java")
OUT = Path(r"D:\CC\ChaosCraft\dist\fluffy_dragdrop\plugins\ChaosCraft\modes\fluffy\fluffy.yml")

CURRENT_CONFIG_VERSION = 4  # mirrors FluffyConfig.CURRENT_CONFIG_VERSION


# ---------------------------------------------------------------------------
# Java parsing helpers
# ---------------------------------------------------------------------------

_ESCAPE_MAP = {
    'n': '\n', 't': '\t', 'r': '\r',
    '"': '"', "'": "'", '\\': '\\',
    'b': '\b', 'f': '\f', '0': '\0',
}


def _decode_java_string_escapes(raw: str) -> str:
    """Decode Java string-literal backslash escapes (\\n, \\", \\\\, \\u####).
    Leaves all other (multibyte/UTF-8) characters intact."""
    out: list[str] = []
    i = 0
    while i < len(raw):
        c = raw[i]
        if c != '\\':
            out.append(c)
            i += 1
            continue
        if i + 1 >= len(raw):
            out.append(c)
            i += 1
            continue
        nxt = raw[i + 1]
        if nxt == 'u' and i + 5 < len(raw):
            hex4 = raw[i + 2:i + 6]
            try:
                out.append(chr(int(hex4, 16)))
                i += 6
                continue
            except ValueError:
                pass
        if nxt in _ESCAPE_MAP:
            out.append(_ESCAPE_MAP[nxt])
            i += 2
            continue
        # unknown escape: keep literal
        out.append(nxt)
        i += 2
    return "".join(out)


def _strip_comments(java: str) -> str:
    """Remove // line comments and /* block */ comments."""
    java = re.sub(r"/\*[\s\S]*?\*/", "", java)
    out = []
    for line in java.splitlines():
        # remove trailing // comment but be careful not to strip inside strings
        in_str = False
        prev = ''
        idx = None
        i = 0
        while i < len(line):
            c = line[i]
            if c == '"' and prev != '\\':
                in_str = not in_str
            if not in_str and c == '/' and i + 1 < len(line) and line[i + 1] == '/':
                idx = i
                break
            prev = c
            i += 1
        if idx is not None:
            line = line[:idx]
        out.append(line)
    return "\n".join(out)


def _extract_method_body(src: str, signature_regex: str) -> str:
    m = re.search(signature_regex, src)
    if not m:
        raise SystemExit(f"Could not find method matching: {signature_regex}")
    # find first { after match end
    i = src.find("{", m.end())
    if i < 0:
        raise SystemExit("No opening brace after method signature")
    depth = 0
    j = i
    while j < len(src):
        c = src[j]
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return src[i + 1:j]
        j += 1
    raise SystemExit("Unbalanced braces in method body")


def _parse_value(token: str) -> Any:
    """Parse a Java literal expression as a python value, best-effort."""
    s = token.strip().rstrip(';').strip()
    # CURRENT_CONFIG_VERSION
    if s == "CURRENT_CONFIG_VERSION":
        return CURRENT_CONFIG_VERSION
    # boolean
    if s == "true":
        return True
    if s == "false":
        return False
    # numeric
    if re.fullmatch(r"-?\d+", s):
        return int(s)
    if re.fullmatch(r"-?\d+\.\d+[dDfF]?", s):
        return float(s.rstrip("dDfF"))
    if re.fullmatch(r"-?\d+[dDfF]", s):
        return float(s.rstrip("dDfF"))
    # string literal
            # string literal
    m = re.fullmatch(r'"((?:[^"\\]|\\.)*)"', s)
    if m:
        return _decode_java_string_escapes(m.group(1))
    # new ArrayList<>()
    if "new ArrayList" in s:
        return []
    # Arrays.asList(...) of strings
    m = re.fullmatch(r"Arrays\.asList\((.*)\)", s, re.DOTALL)
    if m:
        return _parse_string_list(m.group(1))
    # List.of(...) of strings
    m = re.fullmatch(r"List\.of\((.*)\)", s, re.DOTALL)
    if m:
        return _parse_string_list(m.group(1))
    # buildDefaultRainMobs() — handled specially upstream
    if s == "buildDefaultRainMobs()":
        return ("__BUILD_RAIN_MOBS__",)
    raise ValueError(f"Cannot parse Java value: {token!r}")


def _parse_string_list(args: str) -> list[str]:
    """Parse a comma-separated list of double-quoted Java strings, allowing
    line breaks and concatenation across statement-call args.
    """
    out: list[str] = []
    i = 0
    s = args
    while i < len(s):
        c = s[i]
        if c.isspace() or c == ',':
            i += 1
            continue
        if c == '"':
            # parse one string literal, possibly concatenated with + "..."
            buf = []
            while i < len(s) and s[i] == '"':
                # consume string
                j = i + 1
                lit = []
                while j < len(s):
                    if s[j] == '\\' and j + 1 < len(s):
                        lit.append(s[j:j + 2])
                        j += 2
                        continue
                    if s[j] == '"':
                        break
                    lit.append(s[j])
                    j += 1
                buf.append("".join(lit))
                i = j + 1  # skip closing quote
                # skip whitespace/+ to allow "abc" + "def" concatenation
                k = i
                while k < len(s) and (s[k].isspace() or s[k] == '+'):
                    k += 1
                if k < len(s) and s[k] == '"':
                    i = k
                    continue
                break
            raw = "".join(buf)
            decoded = _decode_java_string_escapes(raw)
            out.append(decoded)
            continue
        # bare token (e.g. nested call) — bail
        raise ValueError(f"Unexpected token in string list at: {s[i:i+30]!r}")
    return out


# ---------------------------------------------------------------------------
# Walk the createDefaults() body in source order, emitting (op, key, payload)
# events. op in {"set", "comments"}.
# ---------------------------------------------------------------------------

def parse_defaults_body(body: str) -> list[tuple]:
    """Return ordered events. Handles multi-line statements by reading
    each statement (terminated by `;` at brace depth 0)."""
    events: list[tuple] = []
    # Tokenise into statements.
    stmts: list[str] = []
    depth_paren = 0
    depth_brace = 0
    buf = []
    in_str = False
    prev = ''
    i = 0
    while i < len(body):
        c = body[i]
        # handle strings
        if in_str:
            buf.append(c)
            if c == '"' and prev != '\\':
                in_str = False
            prev = c
            i += 1
            continue
        if c == '"' and prev != '\\':
            in_str = True
            buf.append(c)
            prev = c
            i += 1
            continue
        if c == '(':
            depth_paren += 1
        elif c == ')':
            depth_paren -= 1
        elif c == '{':
            depth_brace += 1
        elif c == '}':
            depth_brace -= 1
        if c == ';' and depth_paren == 0 and depth_brace == 0:
            stmts.append("".join(buf))
            buf = []
            prev = c
            i += 1
            continue
        buf.append(c)
        prev = c
        i += 1
    tail = "".join(buf).strip()
    if tail:
        stmts.append(tail)

    # Accept either a plain "key" string OR a ROOT + ".key" concatenation
    # (used by MobSpawnConfig.writeDefaults).
    KEY = r'(?:"([^"]+)"|ROOT\s*\+\s*"\.([^"]+)")'
    set_re = re.compile(
        r'^\s*defaults\.set\(\s*' + KEY + r'\s*,\s*([\s\S]+?)\s*\)\s*$'
    )
    cmt_re = re.compile(
        r'^\s*defaults\.setComments\(\s*' + KEY + r'\s*,\s*List\.of\(\s*([\s\S]*?)\s*\)\s*\)\s*$'
    )
    mob_call_re = re.compile(
        r'^\s*MobSpawnConfig\.writeDefaults\(\s*defaults\s*,\s*new\s+ArrayList<>\(\s*\)\s*\)\s*$'
    )

    def _extract_key(m: re.Match, root_prefix: str = "mob-spawning") -> str:
        plain = m.group(1)
        if plain is not None:
            return plain
        suffix = m.group(2)
        return f"{root_prefix}.{suffix}"

    for stmt in stmts:
        s = stmt.strip()
        if not s:
            continue
        m = cmt_re.match(s)
        if m:
            key = _extract_key(m)
            inner = m.group(3)
            lines = _parse_string_list(inner)
            events.append(("comments", key, lines))
            continue
        m = set_re.match(s)
        if m:
            key = _extract_key(m)
            val_src = m.group(3)
            try:
                val = _parse_value(val_src)
            except ValueError:
                # Could not parse RHS (e.g. mobListMaps variable). Treat as
                # an empty list and let the caller patch via ensure_keys.
                val = []
            events.append(("set", key, val))
            continue
        if mob_call_re.match(s):
            events.append(("call_mobspawn", None, None))
            continue
        # Ignore other statements (try/catch, save).
    return events


# ---------------------------------------------------------------------------
# Build mob-spawning events from MobSpawnConfig.writeDefaults
# ---------------------------------------------------------------------------

def parse_mobspawn_events() -> list[tuple]:
    src = _strip_comments(JAVA_MOBSPAWN.read_text(encoding="utf-8"))
    body = _extract_method_body(
        src,
        r"public\s+static\s+void\s+writeDefaults\s*\("
    )
    events = parse_defaults_body(body)
    # Replace defaults.set(ROOT + ".mobs", mobListMaps) — for empty input
    # this is set() with the variable mobListMaps. parse_defaults_body
    # would fail to parse because the value is `mobListMaps`. Manually
    # add a synthesised event after extracting only the static keys.
    cleaned: list[tuple] = []
    saw_mobs = False
    for ev in events:
        op, key, payload = ev
        if op == "set" and key and key.endswith(".mobs"):
            cleaned.append(("set", key, []))
            saw_mobs = True
            continue
        cleaned.append(ev)
    # if the parser couldn't even produce a .mobs set due to the unparsable
    # `mobListMaps` value, the comments event will still exist. Inject a
    # set(.mobs, []) right before it.
    if not saw_mobs:
        # find mobs comments
        for idx, ev in enumerate(cleaned):
            if ev[0] == "comments" and ev[1].endswith(".mobs"):
                cleaned.insert(idx, ("set", "mob-spawning.mobs", []))
                break
    return cleaned


# ---------------------------------------------------------------------------
# buildDefaultRainMobs — replicate the helper output literally.
# ---------------------------------------------------------------------------

def build_rain_mobs() -> list[dict]:
    src = _strip_comments(JAVA_FLUFFY.read_text(encoding="utf-8"))
    body = _extract_method_body(
        src,
        r"private\s+List<Map<String,\s*Object>>\s+buildDefaultRainMobs\s*\(\s*\)"
    )
    out: list[dict] = []
    # Match list.add(mmMob("ID", weight))
    mm_re = re.compile(r'list\.add\(mmMob\(\s*"([^"]+)"\s*,\s*(\d+)\s*\)\)')
    van_re = re.compile(r'list\.add\(vanillaMob\(\s*"([^"]+)"\s*,\s*(\d+)\s*\)\)')
    # Iterate line-by-line preserving order.
    for line in body.splitlines():
        m = mm_re.search(line)
        if m:
            out.append({
                "id": m.group(1),
                "type": "mythicmobs",
                "weight": int(m.group(2)),
                "min-count": 1,
                "max-count": 1,
                "health-multiplier": 1.0,
                "damage-multiplier": 1.0,
            })
            continue
        m = van_re.search(line)
        if m:
            out.append({
                "id": m.group(1),
                "type": "vanilla",
                "weight": int(m.group(2)),
                "min-count": 1,
                "max-count": 1,
                "health-multiplier": 1.5,
                "damage-multiplier": 1.5,
            })
    return out


# ---------------------------------------------------------------------------
# Build a tree of (key -> value | subtree) preserving insertion order, and
# attach pending comment lists to each leaf+section.
# ---------------------------------------------------------------------------

class Node:
    __slots__ = ("kind", "value", "children", "comments")

    def __init__(self, kind: str, value: Any = None) -> None:
        # kind: "section" | "leaf"
        self.kind = kind
        self.value = value
        self.children: dict[str, "Node"] = {}
        self.comments: list[str] = []


def insert(root: Node, dotted: str, value: Any, comments: list[str] | None) -> None:
    parts = dotted.split(".")
    node = root
    for i, p in enumerate(parts):
        last = i == len(parts) - 1
        if last:
            child = Node("leaf", value)
            child.comments = comments or []
            node.children[p] = child
        else:
            if p not in node.children:
                node.children[p] = Node("section")
            elif node.children[p].kind != "section":
                # promote leaf into section (shouldn't happen for our schema)
                old = node.children[p]
                section = Node("section")
                section.comments = old.comments
                node.children[p] = section
            node = node.children[p]


def attach_comments(root: Node, dotted: str, comments: list[str]) -> None:
    """Attach to existing leaf or section. If neither exists, this is the
    "first comment under a parent section" case — attach to the deepest
    existing ancestor section header."""
    parts = dotted.split(".")
    node = root
    for i, p in enumerate(parts):
        if p not in node.children:
            return
        child = node.children[p]
        if i == len(parts) - 1:
            child.comments = comments
            return
        node = child


# ---------------------------------------------------------------------------
# YAML emission
# ---------------------------------------------------------------------------

def _yaml_scalar(v: Any) -> str:
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, int):
        return str(v)
    if isinstance(v, float):
        # Bukkit emits 1.0, 0.5, etc. with a single decimal; keep that style
        if v == int(v):
            return f"{v:.1f}"
        return repr(v)
    if v is None:
        return "null"
    if isinstance(v, str):
        # Empty string -> ''
        if v == "":
            return "''"
        # Bukkit quotes strings that contain special chars or look numeric
        if re.fullmatch(r"[-+]?\d+(\.\d+)?", v):
            return f"'{v}'"
        if v.lower() in ("true", "false", "null", "yes", "no", "on", "off"):
            return f"'{v}'"
        if any(c in v for c in ":#\n\"'") or v.startswith((" ", "-")):
            return "'" + v.replace("'", "''") + "'"
        # Preserve identifiers that start with - (e.g. dashed) by quoting
        return v
    raise TypeError(f"Cannot scalarise: {v!r}")


def emit_comments(comments: list[str], indent: str, out: list[str]) -> None:
    for line in comments:
        if line == "":
            out.append("")
        else:
            out.append(f"{indent}# {line}")


def emit_node(name: str, node: Node, indent_lvl: int, out: list[str]) -> None:
    indent = "  " * indent_lvl
    emit_comments(node.comments, indent, out)
    if node.kind == "section":
        out.append(f"{indent}{name}:")
        for child_name, child in node.children.items():
            emit_node(child_name, child, indent_lvl + 1, out)
        return
    # leaf
    v = node.value
    if isinstance(v, list):
        if not v:
            out.append(f"{indent}{name}: []")
            return
        # list of maps (rain mobs) or list of strings
        if all(isinstance(x, dict) for x in v):
            out.append(f"{indent}{name}:")
            child_indent = "  " * (indent_lvl + 1)
            for entry in v:
                first = True
                for k, val in entry.items():
                    if first:
                        out.append(f"{child_indent}- {k}: {_yaml_scalar(val)}")
                        first = False
                    else:
                        out.append(f"{child_indent}  {k}: {_yaml_scalar(val)}")
            return
        # list of strings/scalars
        out.append(f"{indent}{name}:")
        child_indent = "  " * (indent_lvl + 1)
        for x in v:
            out.append(f"{child_indent}- {_yaml_scalar(x)}")
        return
    out.append(f"{indent}{name}: {_yaml_scalar(v)}")


# ---------------------------------------------------------------------------
# Driver
# ---------------------------------------------------------------------------

def main() -> int:
    src = _strip_comments(JAVA_FLUFFY.read_text(encoding="utf-8"))
    body = _extract_method_body(
        src,
        r"private\s+void\s+createDefaults\s*\(\s*\)"
    )
    events = parse_defaults_body(body)

    # Inline MobSpawnConfig.writeDefaults events at the call site.
    mobspawn_events = parse_mobspawn_events()
    expanded: list[tuple] = []
    for ev in events:
        if ev[0] == "call_mobspawn":
            expanded.extend(mobspawn_events)
        else:
            expanded.append(ev)
    events = expanded

    # Resolve __BUILD_RAIN_MOBS__ sentinel.
    rain_mobs = build_rain_mobs()
    fixed: list[tuple] = []
    for op, key, payload in events:
        if op == "set" and isinstance(payload, tuple) and payload == ("__BUILD_RAIN_MOBS__",):
            fixed.append(("set", key, rain_mobs))
        else:
            fixed.append((op, key, payload))
    events = fixed

    # Build tree. Apply set then comments in order.
    root = Node("section")
    pending_comments: dict[str, list[str]] = {}

    for op, key, payload in events:
        if op == "set":
            insert(root, key, payload, pending_comments.pop(key, None))
        elif op == "comments":
            # If the leaf/section exists already, attach. Otherwise stash for
            # when it's set.
            attach_comments(root, key, payload)
            # also stash in case set() comes later
            pending_comments[key] = payload

    out: list[str] = []
    for child_name, child in root.children.items():
        emit_node(child_name, child, 0, out)
    text = "\n".join(out) + "\n"

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(text, encoding="utf-8")

    # Stats
    line_count = text.count("\n")
    comment_count = sum(1 for ln in text.splitlines() if ln.lstrip().startswith("#"))
    print(f"Wrote {OUT}")
    print(f"  Lines: {line_count}")
    print(f"  Comment lines: {comment_count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
