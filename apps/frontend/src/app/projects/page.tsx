'use client';

import { ProtectedRoute } from '@/components/ProtectedRoute';
import { ProjectList } from '@/components/projects/ProjectList';

export default function ProjectsPage() {
  return (
    <ProtectedRoute>
      <ProjectList />
    </ProtectedRoute>
  );
}
