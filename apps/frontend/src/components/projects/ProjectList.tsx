'use client';

import { useEffect, useRef, useState } from 'react';
import { Project } from '@/lib/projects-api';
import { useProjectsStore } from '@/stores/projects-store';
import { ProjectCard } from '@/components/projects/ProjectCard';
import { ProjectDeleteDialog } from '@/components/projects/ProjectDeleteDialog';
import { ProjectForm } from '@/components/projects/ProjectForm';

type FormMode = 'create' | 'edit' | null;

const primaryButton =
  'inline-flex min-h-[40px] items-center justify-center rounded-md bg-accent px-4 py-2.5 font-medium text-accent-contrast transition-colors hover:bg-accent-hover active:bg-accent-active focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40 disabled:cursor-not-allowed disabled:opacity-60';
const secondaryButton =
  'inline-flex min-h-[40px] items-center justify-center rounded-md border border-border bg-surface px-4 py-2.5 font-medium text-ink transition-colors hover:bg-surface-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40 disabled:cursor-not-allowed disabled:opacity-60';

export function ProjectList() {
  const { projects, isLoading, error, fetchProjects, createProject, updateProject, deleteProject } =
    useProjectsStore();
  const [mode, setMode] = useState<FormMode>(null);
  const [editing, setEditing] = useState<Project | null>(null);
  const [deleting, setDeleting] = useState<Project | null>(null);
  const newProjectRef = useRef<HTMLButtonElement>(null);
  const deleteTriggerRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    void fetchProjects();
  }, [fetchProjects]);

  const openCreate = () => {
    setEditing(null);
    setMode('create');
  };

  const openEdit = (project: Project) => {
    setEditing(project);
    setMode('edit');
  };

  const closeForm = () => {
    setMode(null);
    setEditing(null);
  };

  const handleCreateSuccess = () => {
    closeForm();
    newProjectRef.current?.focus();
  };

  const handleEditSuccess = () => {
    const editedId = editing?.id;
    closeForm();
    if (editedId) {
      document.getElementById(`project-edit-${editedId}`)?.focus();
    }
  };

  const handleArchive = async (project: Project) => {
    await updateProject(project.id, { status: 'ARCHIVED' });
  };

  const handleReactivate = async (project: Project) => {
    await updateProject(project.id, { status: 'ACTIVE' });
  };

  const openDelete = (project: Project) => {
    deleteTriggerRef.current = document.activeElement as HTMLElement | null;
    setDeleting(project);
  };

  const closeDelete = () => {
    setDeleting(null);
    deleteTriggerRef.current?.focus();
  };

  const handleDeleteConfirm = async () => {
    if (!deleting) {
      return;
    }
    await deleteProject(deleting.id);
    setDeleting(null);
    document.getElementById('projects-heading')?.focus();
  };

  const projectList =
    projects.length > 0 ? (
      <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {projects.map((project) => (
          <li key={project.id}>
            <ProjectCard
              project={project}
              onEdit={openEdit}
              onArchive={handleArchive}
              onReactivate={handleReactivate}
              onDelete={openDelete}
            />
          </li>
        ))}
      </ul>
    ) : null;

  return (
    <main className="min-h-screen bg-canvas text-ink">
      <div className="mx-auto w-full max-w-[1200px] px-6 py-12 md:px-8">
        <header className="mb-8 flex items-start justify-between gap-6">
          <div>
            <h1
              id="projects-heading"
              tabIndex={-1}
              className="font-display text-headline text-ink focus:outline-none"
            >
              Projects
            </h1>
            <p className="mt-2 text-body text-ink-secondary">Organize your time by context.</p>
          </div>
          <button
            ref={newProjectRef}
            type="button"
            onClick={openCreate}
            className={primaryButton}
          >
            New project
          </button>
        </header>

        {mode ? (
          <div className="mb-8">
            <ProjectForm
              mode={mode}
              project={editing}
              onCancel={closeForm}
              onSuccess={mode === 'edit' ? handleEditSuccess : handleCreateSuccess}
            />
          </div>
        ) : null}

        {isLoading ? (
          <p role="status" className="py-2xl text-center text-body text-ink-secondary">
            Loading projects…
          </p>
        ) : error ? (
          <div
            role="alert"
            className="rounded-md border border-danger/30 bg-danger-subtle p-4 text-body text-danger"
          >
            <p>{error}</p>
            <button
              type="button"
              onClick={() => void fetchProjects()}
              className={`${secondaryButton} mt-3`}
            >
              Retry
            </button>
          </div>
        ) : projects.length === 0 ? (
          <div className="rounded-lg border border-border bg-surface p-6 text-center shadow-sm">
            <h2 className="font-display text-title text-ink">No projects yet</h2>
            <p className="mt-2 text-body text-ink-secondary">
              Create your first project to organize your time blocks.
            </p>
            <button
              type="button"
              onClick={openCreate}
              className={`${primaryButton} mt-4`}
            >
              New project
            </button>
          </div>
        ) : null}

        {!isLoading && projects.length > 0 ? <div>{projectList}</div> : null}
      </div>

      {deleting ? (
        <ProjectDeleteDialog
          project={deleting}
          onCancel={closeDelete}
          onConfirm={handleDeleteConfirm}
        />
      ) : null}
    </main>
  );
}
