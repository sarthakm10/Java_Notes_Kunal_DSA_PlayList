"""Build web/data.js for the local study site (index.html).

Reads java_dsa_study_roadmap.md, the notes folders (titles only: notes open on GitHub) and
web/practice-data.json, and writes web/data.js. Run it again after editing the roadmap or adding notes:

    python tools/build_site.py
"""
import json, os, re, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROADMAP = 'java_dsa_study_roadmap.md'
NOTE_FOLDERS = ['kunal-java-notes-claude', 'DSA Topics Not Covered Notes', 'Java Topics Not Covered Notes', 'Spring Boot Notes']
ROOT_DOCS = ['java_dsa_study_roadmap.md', 'topics_not_covered.md', 'dsa_practice_sheet.md']
TOPIC_SECTIONS = ('Phase', 'Parallel', 'Track')
SKIP_SECTIONS = ('Table of Contents', 'Progress Checklist')


def read(rel):
    with open(os.path.join(ROOT, rel), encoding='utf-8') as f:
        return f.read().replace('\r\n', '\n')


def slug(h):
    s = re.sub(r'[^\w\- ]', '', h.strip().lower())
    return s.replace(' ', '-')


def split_row(line):
    cells = line.strip().strip('|').split('|')
    return [c.strip() for c in cells]


def parse_roadmap():
    text = read(ROADMAP)
    refs = dict(re.findall(r'^\[([^\]]+)\]:\s*(\S+)\s*$', text, re.M))
    text = re.sub(r'^\[[^\]]+\]:\s*\S+\s*$\n?', '', text, flags=re.M)
    text = re.sub(r'\[([^\]]+)\]\[([^\]]+)\]', lambda m: f'[{m.group(1)}]({refs[m.group(2)]})' if m.group(2) in refs else m.group(0), text)
    chunks = re.split(r'^## ', text, flags=re.M)
    head = chunks[0]
    title = re.search(r'^# (.+)$', head, re.M).group(1)
    intro = re.sub(r'^# .+\n', '', head).strip()
    intro = re.sub(r'\n---\s*$', '', intro).strip()
    sections = []
    for ch in chunks[1:]:
        name, _, body = ch.partition('\n')
        name = name.strip()
        body = re.sub(r'\n---\s*$', '', body.strip()).strip()
        if name.startswith(SKIP_SECTIONS):
            continue
        sec = {'title': name, 'anchor': slug(name), 'kind': 'topics' if name.startswith(TOPIC_SECTIONS) else 'text'}
        if sec['kind'] == 'text':
            sec['md'] = body
            sections.append(sec)
            continue
        lines = body.split('\n')
        start = next(i for i, l in enumerate(lines) if l.startswith('|'))
        end = start
        while end < len(lines) and lines[end].startswith('|'):
            end += 1
        headers = split_row(lines[start])
        rows = [split_row(l) for l in lines[start + 2:end]]
        m = re.match(r'Phase (\d+)', name)
        num = m.group(1) if m else None
        topics = []
        for i, cells in enumerate(rows, 1):
            row = dict(zip(headers, cells))
            tid = row.get('Order') or f'{num}.{i}'
            pr = re.search(r'🔴|🟡|🟢', row.get('Topic', '') + row.get('Priority', ''))
            topics.append({'id': tid, 'cells': cells, 'priority': pr.group(0) if pr else ''})
        sec.update({'num': num, 'before': '\n'.join(lines[:start]).strip(), 'headers': headers,
                    'topics': topics, 'after': '\n'.join(lines[end:]).strip()})
        sections.append(sec)
    return {'title': title, 'intro': intro, 'sections': sections}


def list_notes():
    out = []
    for rel in ROOT_DOCS:
        if os.path.exists(os.path.join(ROOT, rel)):
            out.append(('Overview', rel))
    for folder in NOTE_FOLDERS:
        d = os.path.join(ROOT, folder)
        for name in sorted(os.listdir(d)):
            if name.endswith('.md'):
                out.append((folder, f'{folder}/{name}'))
    notes = []
    for group, rel in out:
        m = re.search(r'^# (.+)$', read(rel), re.M)
        title = m.group(1).strip() if m else rel
        notes.append({'group': group, 'path': rel, 'title': title.replace(' — Complete In-Depth Guide', '')})
    return notes


def repo_url():
    """GitHub page of this repo (notes links open there), from the git remote 'origin'."""
    try:
        url = subprocess.run(['git', 'config', '--get', 'remote.origin.url'], cwd=ROOT,
                             capture_output=True, text=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        url = ''
    m = re.match(r'(?:https://|git@)github\.com[/:](.+?)(?:\.git)?$', url)
    if not m:
        sys.exit(f'remote.origin.url is not a GitHub repo ({url!r}); notes links need it')
    return 'https://github.com/' + m.group(1)


def main():
    roadmap = parse_roadmap()
    practice = json.loads(read('web/practice-data.json'))
    ids = {t['id'] for s in roadmap['sections'] if s['kind'] == 'topics' for t in s['topics']}
    missing = sorted({p['topic'] for p in practice['problems']} - ids)
    if missing:
        sys.exit(f'practice topics not found in the roadmap: {missing}')
    notes = list_notes()
    paths = {n['path'] for n in notes}
    bad = sorted({f for s in practice['sections'] for f in s['notes']} - paths)
    if bad:
        sys.exit(f'practice sections link missing notes: {bad}')
    data = {'repo': repo_url(), 'roadmap': roadmap, 'practice': practice, 'notes': notes}
    with open(os.path.join(ROOT, 'web', 'data.js'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('// Generated by tools/build_site.py: do not edit by hand.\n')
        f.write('window.STUDY = ' + json.dumps(data, ensure_ascii=False, separators=(',', ':')) + ';\n')
    n_topics = sum(len(s.get('topics', [])) for s in roadmap['sections'])
    print(f"web/data.js: {len(roadmap['sections'])} roadmap sections, {n_topics} topics, "
          f"{len(practice['problems'])} problems, {len(notes)} notes files")


if __name__ == '__main__':
    main()
